package com.carpool.dto.mobile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for mobile verification request
 * Contains Firebase UID and mobile number for backend verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileVerificationRequest {

    @NotBlank(message = "Firebase UID is required")
    private String firebaseUid;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile must be 10 digits")
    private String mobileNumber;

    private String otp;  // Optional: for backend OTP verification if needed
}
