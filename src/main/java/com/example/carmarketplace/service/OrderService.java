package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.CarStatus;
import com.example.carmarketplace.domain.OrderStatus;
import com.example.carmarketplace.domain.PurchaseOrder;
import com.example.carmarketplace.repository.CarRepository;
import com.example.carmarketplace.repository.PurchaseOrderRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final PurchaseOrderRepository orderRepository;
    private final CarRepository carRepository;
    private final AccountService accountService;
    private final CarService carService;
    private final AuditService auditService;

    public OrderService(PurchaseOrderRepository orderRepository,
                        CarRepository carRepository,
                        AccountService accountService,
                        CarService carService,
                        AuditService auditService) {
        this.orderRepository = orderRepository;
        this.carRepository = carRepository;
        this.accountService = accountService;
        this.carService = carService;
        this.auditService = auditService;
    }

    public ApiResponses.OrderView create(String buyerId, ApiRequests.CreateOrderRequest request) {
        Account buyer = accountService.findById(buyerId);
        if (buyer.getRole() != AccountRole.BUYER) {
            throw ApiException.forbidden("Only buyer accounts can create purchase orders");
        }

        Car car = carService.findAnyById(request.carId());
        if (car.getStatus() != CarStatus.ACTIVE) {
            throw ApiException.conflict("This car is no longer available");
        }
        if (car.getSellerId().equals(buyerId)) {
            throw ApiException.badRequest("You cannot create a purchase order for your own listing");
        }

        boolean existingOrder = orderRepository.findAll().stream()
                .anyMatch(order -> order.getBuyerId().equals(buyerId)
                        && order.getCarId().equals(car.getId())
                        && (order.getStatus() == OrderStatus.PENDING
                        || order.getStatus() == OrderStatus.APPROVED));
        if (existingOrder) {
            throw ApiException.conflict("You already have an active order for this car");
        }

        PurchaseOrder order = new PurchaseOrder(UUID.randomUUID().toString(), buyerId,
                car.getSellerId(), car.getId(), car.getPrice());
        List<PurchaseOrder> orders = orderRepository.findAll();
        orders.add(order);
        orderRepository.saveAll(orders);
        auditService.record(buyerId, "ORDER_CREATED", "order", order.getId(), "Purchase order created");
        return toView(order);
    }

    public List<ApiResponses.OrderView> history(String accountId, AccountRole role) {
        return orderRepository.findAll().stream()
                .filter(order -> role == AccountRole.ADMIN
                        || order.getBuyerId().equals(accountId)
                        || order.getSellerId().equals(accountId))
                .sorted(Comparator.comparing(PurchaseOrder::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public ApiResponses.OrderView updateStatus(
            String orderId, String actorId, AccountRole actorRole, OrderStatus requestedStatus) {
        PurchaseOrder order = orderRepository.findAll().stream()
                .filter(existing -> existing.getId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Purchase order was not found"));

        validateTransition(order, actorId, actorRole, requestedStatus);
        if (requestedStatus == OrderStatus.COMPLETED) {
            Car car = carService.findAnyById(order.getCarId());
            if (car.getStatus() != CarStatus.ACTIVE) {
                throw ApiException.conflict("The car is no longer available to complete this order");
            }
            carService.markSold(car);
            cancelOtherOpenOrders(order);
        }

        order.setStatus(requestedStatus);
        order.setUpdatedAt(Instant.now());
        List<PurchaseOrder> orders = orderRepository.findAll();
        orders.removeIf(existing -> existing.getId().equals(order.getId()));
        orders.add(order);
        orderRepository.saveAll(orders);
        auditService.record(actorId, "ORDER_STATUS_CHANGED", "order", order.getId(),
                "Status changed to " + requestedStatus);
        return toView(order);
    }

    public long countAll() {
        return orderRepository.findAll().size();
    }

    public long countPending() {
        return orderRepository.findAll().stream()
                .filter(order -> order.getStatus() == OrderStatus.PENDING).count();
    }

    public boolean hasCompletedPurchase(String buyerId, String carId) {
        return orderRepository.findAll().stream()
                .anyMatch(order -> order.getBuyerId().equals(buyerId)
                        && order.getCarId().equals(carId)
                        && order.getStatus() == OrderStatus.COMPLETED);
    }

    private void validateTransition(
            PurchaseOrder order, String actorId, AccountRole actorRole, OrderStatus target) {
        if (target == OrderStatus.CANCELLED) {
            boolean buyerCancellingOwnPending = actorRole == AccountRole.BUYER
                    && order.getBuyerId().equals(actorId)
                    && order.getStatus() == OrderStatus.PENDING;
            boolean sellerCancellingOwnOpenOrder = actorRole == AccountRole.SELLER
                    && order.getSellerId().equals(actorId)
                    && (order.getStatus() == OrderStatus.PENDING
                    || order.getStatus() == OrderStatus.APPROVED);
            if (buyerCancellingOwnPending || sellerCancellingOwnOpenOrder) {
                return;
            }
            throw ApiException.forbidden("This account cannot cancel the purchase order");
        }

        boolean sellerOwnsOrder = actorRole == AccountRole.SELLER
                && order.getSellerId().equals(actorId);
        if (!sellerOwnsOrder) {
            throw ApiException.forbidden("Only the seller can approve or complete this order");
        }
        if (target == OrderStatus.APPROVED && order.getStatus() == OrderStatus.PENDING) {
            return;
        }
        if (target == OrderStatus.COMPLETED && order.getStatus() == OrderStatus.APPROVED) {
            return;
        }
        throw ApiException.conflict("That order status transition is not allowed");
    }

    private void cancelOtherOpenOrders(PurchaseOrder completedOrder) {
        List<PurchaseOrder> orders = orderRepository.findAll();
        boolean changed = false;
        for (PurchaseOrder other : orders) {
            if (!other.getId().equals(completedOrder.getId())
                    && other.getCarId().equals(completedOrder.getCarId())
                    && (other.getStatus() == OrderStatus.PENDING
                    || other.getStatus() == OrderStatus.APPROVED)) {
                other.setStatus(OrderStatus.CANCELLED);
                other.setUpdatedAt(Instant.now());
                changed = true;
            }
        }
        if (changed) {
            orderRepository.saveAll(orders);
        }
    }

    private ApiResponses.OrderView toView(PurchaseOrder order) {
        Account buyer = accountService.findById(order.getBuyerId());
        Account seller = accountService.findById(order.getSellerId());
        Car car = carService.findAnyById(order.getCarId());
        String label = car.getYear() + " " + car.getBrand() + " " + car.getModel();
        return new ApiResponses.OrderView(order.getId(), order.getBuyerId(), buyer.getFullName(),
                order.getSellerId(), seller.getFullName(), order.getCarId(), label,
                order.getAmount(), order.getStatus(), order.getCreatedAt(), order.getUpdatedAt());
    }
}
