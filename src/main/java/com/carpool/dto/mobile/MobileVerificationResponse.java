package com.carpool.dto.mobile;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for mobile verification response
 * Contains verification status and timestamp
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileVerificationResponse {

    private UUID userId;

    private String mobileNumber;

    private boolean verified;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedDate;

    private String firebaseUid;

    private String message;

    public static MobileVerificationResponse verified(UUID userId, String mobile, LocalDateTime verifiedDate, String firebaseUid) {
        return MobileVerificationResponse.builder()
                .userId(userId)
                .mobileNumber(mobile)
                .verified(true)
                .verifiedDate(verifiedDate)
                .firebaseUid(firebaseUid)
                .message("Mobile number verified successfully")
                .build();
    }

    public static MobileVerificationResponse notVerified(UUID userId, String mobile) {
        return MobileVerificationResponse.builder()
                .userId(userId)
                .mobileNumber(mobile)
                .verified(false)
                .message("Mobile number not verified")
                .build();
    }
}
