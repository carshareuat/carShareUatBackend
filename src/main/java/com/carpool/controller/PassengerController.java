package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.exception.AppException;
import com.carpool.security.AppUserPrincipal;
import com.carpool.security.AuthFacade;
import com.carpool.service.PassengerLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/passengers")
@RequiredArgsConstructor
@Slf4j
public class PassengerController {

    private final PassengerLocationService passengerLocationService;
    private final AuthFacade authFacade;

    @PostMapping("/{passengerId}/location")
    public ApiResponse<?> postLocation(@PathVariable UUID passengerId, @RequestBody Map<String, Object> body) {
        AppUserPrincipal p = authFacade.currentUser();
        if (p.getUserId() == null || !p.getUserId().equals(passengerId) && p.getRole() != com.carpool.entity.Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not allowed to post location for this passenger");
        }
        Double lat = body.get("lat") instanceof Number ? ((Number) body.get("lat")).doubleValue() : null;
        Double lon = body.get("lon") instanceof Number ? ((Number) body.get("lon")).doubleValue() : null;
        if (lat == null || lon == null) { throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "lat and lon required"); }
        try {
            return ApiResponse.of(passengerLocationService.toMap(passengerLocationService.saveOrUpdate(passengerId, lat, lon)));
        } catch (Exception e) {
            // Log the exception for debugging; keep response generic
            log.error("Failed to save passenger location for {}: {}", passengerId, e.getMessage(), e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unable to save location");
        }
    }
}
