package com.carpool.service;

import com.carpool.dto.auth.LoginOtpRequest;
import com.carpool.dto.auth.LoginOtpResponse;
import com.carpool.dto.auth.VerifyLoginOtpRequest;
import com.carpool.dto.auth.VerifyLoginOtpResponse;
import com.carpool.entity.Role;
import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.UserRepository;
import com.carpool.security.JwtService;
import com.carpool.validation.MobileNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpLoginService {

    private static final int OTP_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_SECONDS = 30;
    private static final int MAX_OTP_REQUESTS_PER_HOUR = 5;
    private static final int OTP_LENGTH = 6;

    private final UserRepository userRepository;
    private final MobileNormalizer mobileNormalizer;
    private final JwtService jwtService;

    private final Map<String, OtpSession> otpSessions = new ConcurrentHashMap<>();
    private final Map<String, Integer> otpRequestCount = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> otpRequestWindow = new ConcurrentHashMap<>();

    private static final class OtpSession {
        private final String otp;
        private final LocalDateTime expiresAt;
        private final LocalDateTime issuedAt;
        private int attempts;
        private int lastRequestCount;

        private OtpSession(String otp, LocalDateTime issuedAt, LocalDateTime expiresAt) {
            this.otp = otp;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }

    public LoginOtpResponse checkMobile(LoginOtpRequest request) {
        String mobile = normalize(request.getMobileNumber());
        boolean exists = userRepository.findByMobile(mobile).isPresent();
        if (!exists) {
            return LoginOtpResponse.builder().exists(false).message("Mobile number is not registered. Please create an account.").build();
        }
        return LoginOtpResponse.builder().exists(true).message("Mobile number found").build();
    }

    @Transactional
    public void sendLoginOtp(LoginOtpRequest request) {
        String mobile = normalize(request.getMobileNumber());
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "MOBILE_NOT_FOUND", "Mobile number is not registered. Please create an account."));

        validateRateLimit(mobile);

        String otp = generateOtp();
        LocalDateTime now = LocalDateTime.now();
        otpSessions.put(mobile, new OtpSession(otp, now, now.plusMinutes(OTP_TTL_MINUTES)));

        user.setLastOtpRequest(now);
        user.setOtpVerified(false);
        userRepository.save(user);

        log.info("OTP sent to mobile {} for user {}", mask(mobile), user.getId());
    }

    @Transactional
    public VerifyLoginOtpResponse verifyLoginOtp(VerifyLoginOtpRequest request) {
        String mobile = normalize(request.getMobileNumber());
        User user = userRepository.findByMobile(mobile)
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "MOBILE_NOT_FOUND", "Mobile number is not registered. Please create an account."));

        OtpSession session = otpSessions.get(mobile);
        if (session == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP expired.");
        }

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            otpSessions.remove(mobile);
            throw new AppException(HttpStatus.BAD_REQUEST, "OTP_EXPIRED", "OTP expired.");
        }

        if (session.attempts >= MAX_ATTEMPTS) {
            otpSessions.remove(mobile);
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "MAX_ATTEMPTS", "Maximum verification attempts exceeded.");
        }

        if (!session.otp.equals(request.getOtp().trim())) {
            session.attempts++;
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_OTP", "Invalid OTP.");
        }

        otpSessions.remove(mobile);
        user.setOtpVerified(true);
        user.setOtpVerifiedOn(LocalDateTime.now());
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        Role effectiveRole = user.getRole() != null ? user.getRole() : Role.PASSENGER;
        String accessToken = jwtService.generateAccessToken(user.getId(), effectiveRole, user.getMobile(), null);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return VerifyLoginOtpResponse.builder()
                .success(true)
                .token(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId())
                .role(effectiveRole)
                .mobile(user.getMobile())
                .message("Login successful")
                .build();
    }

    private void validateRateLimit(String mobile) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = otpRequestWindow.get(mobile);
        if (windowStart == null || Duration.between(windowStart, now).toHours() >= 1) {
            otpRequestWindow.put(mobile, now);
            otpRequestCount.put(mobile, 0);
        }

        int count = otpRequestCount.getOrDefault(mobile, 0);
        if (count >= MAX_OTP_REQUESTS_PER_HOUR) {
            throw new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Please try again later.");
        }

        otpRequestCount.put(mobile, count + 1);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append((int) (Math.random() * 10));
        }
        return otp.toString();
    }

    private String normalize(String mobile) {
        return mobileNormalizer.normalize(mobile);
    }

    private String mask(String mobile) {
        if (mobile == null || mobile.length() <= 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
