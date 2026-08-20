package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.owner.OwnerCreateRequest;
import com.carpool.service.OwnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.web.bind.annotation.RequestBody;
import com.carpool.service.OwnerLocationService;
import java.util.Map;
import com.carpool.security.AuthFacade;
import com.carpool.security.AppUserPrincipal;
import com.carpool.exception.AppException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/owners")
@RequiredArgsConstructor
@Validated
@Slf4j
public class OwnerController {

    private final OwnerService ownerService;
    private final com.carpool.service.RideService rideService;
    private final com.carpool.service.RatingService ratingService;
    private final OwnerLocationService ownerLocationService;
    private final AuthFacade authFacade;

    @PostMapping(consumes = {"multipart/form-data"})
    public ApiResponse<?> create(@ModelAttribute OwnerCreateRequest request) {
        return ApiResponse.of(ownerService.create(request));
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.of(ownerService.list());
    }

    @GetMapping("/{ownerId}")
    public ApiResponse<?> get(@PathVariable UUID ownerId) {
        return ApiResponse.of(ownerService.get(ownerId));
    }

    @PatchMapping("/{ownerId}")
    public ApiResponse<?> patch(@PathVariable UUID ownerId) {
        return ApiResponse.of(ownerService.get(ownerId));
    }

    @GetMapping("/rides")
    public ApiResponse<?> myRides() {
        AppUserPrincipal p = authFacade.currentUser();
        if (p.getOwnerId() == null) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Owner profile required");
        }
        return ApiResponse.of(rideService.byOwner(p.getOwnerId()));
    }

    @GetMapping("/{ownerId}/rides")
    public ApiResponse<?> ownerRides(@PathVariable UUID ownerId) {
        return ApiResponse.of(rideService.byOwner(ownerId));
    }

    @GetMapping("/{ownerId}/ratings")
    public ApiResponse<?> ownerRatings(@PathVariable UUID ownerId) {
        return ApiResponse.of(ratingService.breakdown(ownerId));
    }

    @PostMapping("/{ownerId}/location")
    public ApiResponse<?> postLocation(@PathVariable UUID ownerId, @RequestBody Map<String, Object> body) {
        AppUserPrincipal p = authFacade.currentUser();
        boolean allowed = !(p.getOwnerId() == null || (!p.getOwnerId().equals(ownerId) && p.getRole() != com.carpool.entity.Role.ADMIN));
        if (!allowed) {
            // Log principal info for easier debugging when requests are forbidden
            log.warn("Forbidden location post attempt. Principal: userId={}, ownerId={}, role={}, targetOwnerId={}", p.getUserId(), p.getOwnerId(), p.getRole(), ownerId);
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not allowed to post location for this owner");
        }
        Double lat = body.get("lat") instanceof Number ? ((Number) body.get("lat")).doubleValue() : null;
        Double lon = body.get("lon") instanceof Number ? ((Number) body.get("lon")).doubleValue() : null;
        if (lat == null || lon == null) { throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "lat and lon required"); }
        return ApiResponse.of(ownerLocationService.toMap(ownerLocationService.saveOrUpdate(ownerId, lat, lon)));
    }
}
