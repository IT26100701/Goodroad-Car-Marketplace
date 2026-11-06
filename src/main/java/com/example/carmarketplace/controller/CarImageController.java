package com.example.carmarketplace.controller;

import com.example.carmarketplace.api.ApiResponses;
import com.example.carmarketplace.security.AccountPrincipal;
import com.example.carmarketplace.service.CarImageService;
import com.example.carmarketplace.storage.ImageStorage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
public class CarImageController {
    private final CarImageService carImageService;
    private final ImageStorage imageStorage;

    public CarImageController(CarImageService carImageService, ImageStorage imageStorage) {
        this.carImageService = carImageService;
        this.imageStorage = imageStorage;
    }

    @PostMapping(value = "/api/cars/{carId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponses.CarView> upload(
            @PathVariable String carId,
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam("files") List<MultipartFile> files) throws IOException {
        List<byte[]> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            uploads.add(file.getBytes());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carImageService.add(carId, principal.getAccount().getId(), uploads));
    }

    @DeleteMapping("/api/cars/{carId}/images/{fileName:.+}")
    @PreAuthorize("hasRole('SELLER')")
    public ApiResponses.CarView remove(
            @PathVariable String carId,
            @PathVariable String fileName,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return carImageService.remove(carId, principal.getAccount().getId(), fileName);
    }

    @GetMapping("/api/images/{fileName:.+}")
    public ResponseEntity<byte[]> image(@PathVariable String fileName) {
        byte[] data = imageStorage.read(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imageStorage.contentTypeFor(fileName)))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(data);
    }
}
