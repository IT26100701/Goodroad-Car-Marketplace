package com.example.carmarketplace.service;

import com.example.carmarketplace.api.ApiException;
import com.example.carmarketplace.api.ApiRequests;
import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.domain.Account;
import com.example.carmarketplace.domain.AccountRole;
import com.example.carmarketplace.domain.Car;
import com.example.carmarketplace.domain.Review;
import com.example.carmarketplace.domain.ReviewStatus;
import com.example.carmarketplace.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final AccountService accountService;
    private final CarService carService;
    private final OrderService orderService;
    private final AuditService auditService;

    public ReviewService(ReviewRepository reviewRepository,
                        AccountService accountService,
                        CarService carService,
                        OrderService orderService,
                        AuditService auditService) {
        this.reviewRepository = reviewRepository;
        this.accountService = accountService;
        this.carService = carService;
        this.orderService = orderService;
        this.auditService = auditService;
    }

    public List<ApiResponses.ReviewView> listPublishedForCar(String carId) {
        carService.findAnyById(carId);
        return reviewRepository.findAll().stream()
                .filter(review -> review.getCarId().equals(carId)
                        && review.getStatus() == ReviewStatus.PUBLISHED)
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public ApiResponses.ReviewView create(String buyerId, String carId, ApiRequests.ReviewRequest request) {
        Account buyer = requireBuyer(buyerId);
        carService.findAnyById(carId);
        if (!orderService.hasCompletedPurchase(buyerId, carId)) {
            throw ApiException.forbidden("A completed purchase is required before reviewing this car");
        }
        List<Review> reviews = reviewRepository.findAll();
        boolean alreadyReviewed = reviews.stream()
                .anyMatch(review -> review.getBuyerId().equals(buyerId) && review.getCarId().equals(carId));
        if (alreadyReviewed) {
            throw ApiException.conflict("You have already reviewed this car");
        }

        Review review = new Review(UUID.randomUUID().toString(), buyer.getId(), carId,
                request.rating(), request.comment().trim());
        reviews.add(review);
        reviewRepository.saveAll(reviews);
        return toView(review);
    }

    public ApiResponses.ReviewView update(
            String reviewId, String buyerId, ApiRequests.ReviewRequest request) {
        requireBuyer(buyerId);
        Review review = findById(reviewId);
        if (!review.getBuyerId().equals(buyerId)) {
            throw ApiException.forbidden("You can only edit your own reviews");
        }
        review.setRating(request.rating());
        review.setComment(request.comment().trim());
        review.setUpdatedAt(Instant.now());
        save(review);
        return toView(review);
    }

    public void delete(String reviewId, String actorId, AccountRole actorRole) {
        Review review = findById(reviewId);
        if (actorRole != AccountRole.ADMIN && !review.getBuyerId().equals(actorId)) {
            throw ApiException.forbidden("You can only delete your own reviews");
        }
        List<Review> reviews = reviewRepository.findAll();
        reviews.removeIf(existing -> existing.getId().equals(reviewId));
        reviewRepository.saveAll(reviews);
        if (actorRole == AccountRole.ADMIN) {
            auditService.record(actorId, "REVIEW_DELETED", "review", reviewId,
                    "Review removed by administrator");
        }
    }

    public ApiResponses.ReviewView moderate(
            String reviewId, String adminId, ApiRequests.ReviewModerationRequest request) {
        Review review = findById(reviewId);
        review.setStatus(request.status());
        review.setUpdatedAt(Instant.now());
        save(review);
        auditService.record(adminId, "REVIEW_MODERATED", "review", reviewId,
                "Review status set to " + request.status());
        return toView(review);
    }

    public long countAll() {
        return reviewRepository.findAll().size();
    }

    public long countPublished() {
        return reviewRepository.findAll().stream()
                .filter(review -> review.getStatus() == ReviewStatus.PUBLISHED).count();
    }

    public List<ApiResponses.ReviewView> listAllForAdmin() {
        return reviewRepository.findAll().stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    private Account requireBuyer(String buyerId) {
        Account account = accountService.findById(buyerId);
        if (account.getRole() != AccountRole.BUYER) {
            throw ApiException.forbidden("Only buyer accounts can submit reviews");
        }
        return account;
    }

    private Review findById(String id) {
        return reviewRepository.findAll().stream()
                .filter(review -> review.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Review was not found"));
    }

    private void save(Review review) {
        List<Review> reviews = reviewRepository.findAll();
        reviews.removeIf(existing -> existing.getId().equals(review.getId()));
        reviews.add(review);
        reviewRepository.saveAll(reviews);
    }

    private ApiResponses.ReviewView toView(Review review) {
        Account buyer = accountService.findById(review.getBuyerId());
        return new ApiResponses.ReviewView(review.getId(), review.getBuyerId(),
                buyer.getFullName(), review.getCarId(), review.getRating(), review.getComment(),
                review.getStatus(), review.getCreatedAt(), review.getUpdatedAt());
    }
}
