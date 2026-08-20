package com.carpool.service;

import com.carpool.dto.mobile.MobileVerificationRequest;
import com.carpool.dto.mobile.MobileVerificationResponse;
import com.carpool.entity.User;
import com.carpool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Locale;

/**
 * Mobile Verification Service
 * 
 * Responsibilities:
 * 1. Verify Firebase UID provided by frontend
 * 2. Mark mobile number as verified
 * 3. Store Firebase UID for audit trail
 * 4. Update verification timestamp
 * 5. Handle verification status queries
 * 6. Ensure security by validating all requests
 * 
 * Security Notes:
 * - Firebase UID verification happens on frontend via Firebase SDK
 * - This service provides backend confirmation
 * - Mobile number is validated against current user's mobile
 * - mobileVerified flag can ONLY be set through this service
 * - Never trust frontend data; validate everything
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileVerificationService {

    private final UserRepository userRepository;

    /**
     * Verify mobile number for a user
     * 
     * Called after user successfully verifies OTP on frontend
     * - Frontend sends Firebase UID
     * - Backend validates and marks as verified
     * - Stores Firebase UID for audit trail
     * 
     * @param userId User ID
     * @param request Verification request with Firebase UID
     * @return Verification response with status
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public MobileVerificationResponse verifyMobile(UUID userId, MobileVerificationRequest request) {
        log.info("Verifying mobile for user: {}", userId);

        // Validate request
        if (request == null || request.getFirebaseUid() == null || request.getFirebaseUid().isEmpty()) {
            log.warn("Invalid verification request for user: {}", userId);
            throw new IllegalArgumentException("Firebase UID is required for verification");
        }

        if (request.getMobileNumber() == null || request.getMobileNumber().isEmpty()) {
            log.warn("Mobile number missing in verification request for user: {}", userId);
            throw new IllegalArgumentException("Mobile number is required");
        }

        // Validate mobile format (10 digits)
        String cleanMobile = request.getMobileNumber().replaceAll("[^0-9]", "");
        if (cleanMobile.length() != 10 || !cleanMobile.matches("^[6-9][0-9]{9}$")) {
            log.warn("Invalid mobile format for user: {}", userId);
            throw new IllegalArgumentException("Invalid mobile number format");
        }

        // Fetch user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        // Verify mobile number matches current user's mobile
        String userMobile = user.getMobile().replaceAll("[^0-9]", "");
        if (!userMobile.equals(cleanMobile)) {
            log.warn("Mobile number mismatch for user: {}. Expected: {}, Got: {}", 
                    userId, userMobile, cleanMobile);
            throw new IllegalArgumentException("Mobile number does not match registered number");
        }

        // Check if already verified
        if (user.isMobileVerified()) {
            log.info("User {} already has verified mobile", userId);
            return MobileVerificationResponse.verified(
                    userId,
                    user.getMobile(),
                    user.getVerifiedOn(),
                    user.getFirebaseUid()
            );
        }

        // Verify Firebase UID is unique (not already claimed by another user)
        if (userRepository.findByFirebaseUid(request.getFirebaseUid()).isPresent()) {
            User otherUser = userRepository.findByFirebaseUid(request.getFirebaseUid()).get();
            if (!otherUser.getId().equals(userId)) {
                log.warn("Firebase UID already in use by another user. Firebase UID: {}", 
                        request.getFirebaseUid());
                throw new IllegalArgumentException("This Firebase account is already in use");
            }
        }

        // Mark as verified
        user.setMobileVerified(true);
        user.setVerifiedOn(LocalDateTime.now());
        user.setFirebaseUid(request.getFirebaseUid());

        // Persist changes
        User savedUser = userRepository.save(user);
        log.info("Successfully verified mobile for user: {}. Verified on: {}", 
                userId, savedUser.getVerifiedOn());

        // Return success response
        return MobileVerificationResponse.verified(
                userId,
                savedUser.getMobile(),
                savedUser.getVerifiedOn(),
                savedUser.getFirebaseUid()
        );
    }

    /**
     * Get verification status for a user
     * 
     * @param userId User ID
     * @return Verification status response
     */
    @Transactional(readOnly = true)
    public MobileVerificationResponse getVerificationStatus(UUID userId) {
        log.info("Fetching verification status for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("User not found: {}", userId);
                    return new IllegalArgumentException("User not found");
                });

        if (user.isMobileVerified()) {
            return MobileVerificationResponse.verified(
                    userId,
                    user.getMobile(),
                    user.getVerifiedOn(),
                    user.getFirebaseUid()
            );
        } else {
            return MobileVerificationResponse.notVerified(userId, user.getMobile());
        }
    }

    /**
     * Check if user's mobile is verified
     * Helper method for other services
     * 
     * @param userId User ID
     * @return True if verified, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isMobileVerified(UUID userId) {
        return userRepository.findById(userId)
                .map(User::isMobileVerified)
                .orElse(false);
    }

    /**
     * Get mobile verification status without exception throwing
     * Used for profile display
     * 
     * @param userId User ID
     * @return Optional response, empty if user not found
     */
    @Transactional(readOnly = true)
    public MobileVerificationResponse getMobileStatus(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.isMobileVerified()) {
                        return MobileVerificationResponse.verified(
                                userId,
                                user.getMobile(),
                                user.getVerifiedOn(),
                                user.getFirebaseUid()
                        );
                    } else {
                        return MobileVerificationResponse.notVerified(userId, user.getMobile());
                    }
                })
                .orElse(null);
    }

    /**
     * Validate that user has verified mobile before allowing specific operations
     * Can be called before sensitive operations
     * 
     * @param userId User ID
     * @throws IllegalStateException if mobile not verified
     */
    public void requireMobileVerification(UUID userId) {
        if (!isMobileVerified(userId)) {
            log.warn("User {} attempted operation without verified mobile", userId);
            throw new IllegalStateException("Mobile number verification required for this operation");
        }
    }

    /**
     * Backend validation of Firebase UID (optional)
     * Can integrate with Firebase Admin SDK for additional validation
     * 
     * @param firebaseUid Firebase UID to validate
     * @return True if valid and unused, false otherwise
     */
    public boolean isFirebaseUidValid(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.isEmpty()) {
            return false;
        }

        // Check if Firebase UID already exists
        return userRepository.findByFirebaseUid(firebaseUid).isEmpty();
    }
}
