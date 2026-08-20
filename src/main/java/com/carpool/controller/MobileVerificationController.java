package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.mobile.MobileVerificationRequest;
import com.carpool.dto.mobile.MobileVerificationResponse;
import com.carpool.security.AuthFacade;
import com.carpool.service.MobileVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Mobile Verification Controller
 * 
 * Handles REST endpoints for mobile number verification
 * 
 * Endpoints:
 * 1. POST /api/mobile/verify - Verify mobile after OTP
 * 2. GET /api/mobile/status/{userId} - Get verification status
 * 
 * Security:
 * - All endpoints require authentication
 * - User can only verify their own mobile
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile")
@RequiredArgsConstructor
public class MobileVerificationController {

    private final MobileVerificationService mobileVerificationService;
    private final AuthFacade authFacade;

    /**
     * Verify mobile number after successful OTP verification
     * 
     * Called when user completes OTP verification on frontend
     * Frontend sends Firebase UID which is validated here
     * 
     * POST /api/mobile/verify
     * {
     *   "firebaseUid": "xxx",
     *   "mobileNumber": "9876543210"
     * }
     * 
     * @param request Verification request with Firebase UID and mobile number
     * @return ApiResponse with verification result
     */
    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MobileVerificationResponse>> verifyMobile(
            @Valid @RequestBody MobileVerificationRequest request) {

        try {
            log.info("Mobile verification request received for mobile: {}", 
                    maskPhoneNumber(request.getMobileNumber()));

            // Extract user ID from authentication context
            UUID userId = authFacade.currentUser().getUserId();

            // Verify mobile
            MobileVerificationResponse response = mobileVerificationService.verifyMobile(userId, request);

            log.info("Mobile verification successful for user: {}", userId);

            return ResponseEntity.ok(ApiResponse.of(response));

        } catch (IllegalArgumentException e) {
            log.warn("Mobile verification validation failed: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Mobile verification failed with exception", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Mobile verification failed. Please try again."));
        }
    }

    /**
     * Get mobile verification status for a user
     * 
     * GET /api/mobile/status/{userId}
     * 
     * Returns whether the user's mobile number is verified
     * Includes verification timestamp and Firebase UID (masked)
     * 
     * @param userId User ID
     * @return ApiResponse with verification status
     */
    @GetMapping("/status/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<MobileVerificationResponse>> getVerificationStatus(
            @PathVariable UUID userId) {

        try {
            log.info("Verification status request for user: {}", userId);

            // Extract user ID from authentication context
            UUID requestingUserId = authFacade.currentUser().getUserId();

            // User can only check their own status
            if (!requestingUserId.equals(userId)) {
                log.warn("Unauthorized access to status for user: {} by user: {}", userId, requestingUserId);
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("You can only check your own verification status"));
            }

            // Get status
            MobileVerificationResponse response = mobileVerificationService.getVerificationStatus(userId);

            log.info("Verification status retrieved for user: {}. Verified: {}", 
                    userId, response.isVerified());

            return ResponseEntity.ok(ApiResponse.of(response));

        } catch (IllegalArgumentException e) {
            log.warn("Status retrieval failed: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            log.error("Status retrieval failed with exception", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve verification status"));
        }
    }

    /**
     * Check if mobile is verified (simplified response)
     * Used by frontend for quick validation
     * 
     * GET /api/mobile/is-verified/{userId}
     * 
     * @param userId User ID
     * @return Simple boolean response
     */
    @GetMapping("/is-verified/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> isVerified(
            @PathVariable UUID userId) {

        try {
            UUID requestingUserId = authFacade.currentUser().getUserId();

            if (!requestingUserId.equals(userId)) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Unauthorized"));
            }

            boolean verified = mobileVerificationService.isMobileVerified(userId);
            return ResponseEntity.ok(ApiResponse.of(verified));

        } catch (Exception e) {
            log.error("Failed to check verification status", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check verification status"));
        }
    }

    // ============= HELPER METHODS =============

    /**
     * Mask phone number for logging (show only last 4 digits)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (cleaned.length() < 4) {
            return "****";
        }
        return "****" + cleaned.substring(cleaned.length() - 4);
    }
}
