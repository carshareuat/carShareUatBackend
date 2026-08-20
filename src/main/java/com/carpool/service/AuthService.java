package com.carpool.service;

import com.carpool.dto.auth.AuthRequest;
import com.carpool.dto.auth.MeResponse;
import com.carpool.dto.auth.TokenResponse;
import com.carpool.entity.RefreshToken;
import com.carpool.entity.Role;
import com.carpool.entity.User;
import com.carpool.entity.KycDocument;
import com.carpool.entity.KycDocumentType;
import com.carpool.exception.AppException;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.RefreshTokenRepository;
import com.carpool.repository.UserRepository;
import com.carpool.repository.KycDocumentRepository;
import com.carpool.security.AppUserPrincipal;
import com.carpool.security.AuthFacade;
import com.carpool.security.JwtService;
import com.carpool.validation.MobileNormalizer;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final FileStorageService fileStorageService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MobileNormalizer mobileNormalizer;
    private final JwtService jwtService;
    private final TokenHashService tokenHashService;
    private final AuthFacade authFacade;
    private final com.carpool.config.AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;
    private final KycDocumentRepository kycDocumentRepository;

    @Transactional
    public TokenResponse register(AuthRequest request, String ipAddress, String userAgent, org.springframework.web.multipart.MultipartFile profilePhoto, org.springframework.web.multipart.MultipartFile governmentIdProof) {
        if (request.getRole() == Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin registration is disabled");
        }
        if (request.getRole() == Role.PASSENGER && (request.getName() == null || request.getName().isBlank())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Passenger name is required");
        }
        if (request.getRole() == Role.PASSENGER && (governmentIdProof == null || governmentIdProof.isEmpty())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Government ID proof is required");
        }
        String mobile = mobileNormalizer.normalize(request.getMobile());
        var existingOpt = userRepository.findByMobile(mobile);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            log.info("Register attempt for mobile={} requestedRole={} existingRole={}", mobile, request.getRole(), existing.getRole());
            if (request.getRole() == Role.PASSENGER && existing.getRole() == Role.OWNER) {
                if (request.getName() != null && (existing.getName() == null || existing.getName().isBlank())) existing.setName(request.getName().trim());
                if (request.getDateOfBirth() != null && existing.getDateOfBirth() == null) { existing.setDateOfBirth(request.getDateOfBirth()); existing.setAge(calculateAge(request.getDateOfBirth())); }
                if (request.getGender() != null && existing.getGender() == null) existing.setGender(request.getGender());
                existing = userRepository.save(existing);
                return issueTokens(existing, Role.PASSENGER, ipAddress, userAgent);
            }

            if (request.getRole() == Role.OWNER && existing.getRole() == Role.PASSENGER) {
                if (request.getName() != null && (existing.getName() == null || existing.getName().isBlank())) existing.setName(request.getName().trim());
                if (request.getDateOfBirth() != null && existing.getDateOfBirth() == null) { existing.setDateOfBirth(request.getDateOfBirth()); existing.setAge(calculateAge(request.getDateOfBirth())); }
                if (request.getGender() != null && existing.getGender() == null) existing.setGender(request.getGender());
                existing.setRole(Role.OWNER);
                existing = userRepository.save(existing);
                return issueTokens(existing, Role.OWNER, ipAddress, userAgent);
            }

            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_USER", "User already exists");
        }

        User user = new User();
        user.setRole(request.getRole());
        user.setMobile(mobile);
        user.setName(request.getName() == null ? null : request.getName().trim());
        user.setGender(request.getGender());
        user.setFirebaseUid(request.getFirebaseUid());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setAge(calculateAge(request.getDateOfBirth()));
        if (request.getRole() == Role.PASSENGER) {
            // passenger must provide live profile photo
            if (profilePhoto == null || profilePhoto.isEmpty()) {
                throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Passenger profile photo is required");
            }
            String stored = fileStorageService.storePublicProfile(profilePhoto);
            user.setProfilePhotoUrl(stored);
        }
        user = userRepository.save(user);

        if (governmentIdProof != null && !governmentIdProof.isEmpty()) {
            KycDocument document = new KycDocument();
            document.setUser(user);
            document.setType(KycDocumentType.GOVERNMENT_ID);
            document.setPrivateFile(true);
            document.setMimeType(governmentIdProof.getContentType() == null ? "application/octet-stream" : governmentIdProof.getContentType());
            document.setSizeBytes(governmentIdProof.getSize());
            document.setStoragePath(fileStorageService.storePrivateKyc(governmentIdProof));
            kycDocumentRepository.save(document);
        }

        return issueTokens(user, request.getRole(), ipAddress, userAgent);
    }

    @Transactional
    public TokenResponse login(AuthRequest request, String ipAddress, String userAgent) {
        String mobile = mobileNormalizer.normalize(request.getMobile());
        User user = userRepository.findByMobile(mobile)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Mobile number is not registered"));
        if (user.getRole() != request.getRole() && !(request.getRole() == Role.PASSENGER && user.getRole() == Role.OWNER)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Role mismatch");
        }
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);
        return issueTokens(user, request.getRole(), ipAddress, userAgent);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenRaw, String ipAddress, String userAgent) {
        Claims claims;
        try {
            claims = jwtService.parse(refreshTokenRaw);
        } catch (Exception e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid refresh token");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        String tokenHash = tokenHashService.hash(refreshTokenRaw);
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Refresh token revoked"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Refresh token expired");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(user, user.getRole(), ipAddress, userAgent);
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        String tokenHash = tokenHashService.hash(refreshTokenRaw);
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    public MeResponse me() {
        AppUserPrincipal user = authFacade.currentUser();
        com.carpool.entity.User u = userRepository.findById(user.getUserId()).orElse(null);
        String photo = null;
        if (u != null && u.getProfilePhotoUrl() != null) {
            photo = fileExists(u.getProfilePhotoUrl()) ? u.getProfilePhotoUrl() : null;
        } else if (user.getOwnerId() != null) {
            photo = ownerProfileRepository.findById(user.getOwnerId()).map(o -> o.getProfilePhotoUrl()).filter(this::fileExists).orElse(null);
        }
        return MeResponse.builder()
            .userId(user.getUserId())
            .role(user.getRole())
            .mobile(user.getMobile())
            .ownerId(user.getOwnerId())
            .gender(u == null ? null : u.getGender())
            .name(u == null ? null : u.getName())
            .age(u == null ? null : u.getAge())
                .profilePhotoUrl(photo)
            .build();
    }

    private TokenResponse issueTokens(User user, Role effectiveRole, String ipAddress, String userAgent) {
        UUID ownerId = ownerProfileRepository.findByUserId(user.getId()).map(o -> o.getId()).orElse(null);
        String accessToken = jwtService.generateAccessToken(user.getId(), effectiveRole, user.getMobile(), ownerId);
        String refreshTokenRaw = jwtService.generateRefreshToken(user.getId());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHashService.hash(refreshTokenRaw));
        refreshToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
        refreshToken.setIpAddress(ipAddress);
        refreshToken.setUserAgent(userAgent);
        refreshTokenRepository.save(refreshToken);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshTokenRaw)
            .userId(user.getId())
            .role(effectiveRole)
            .mobile(user.getMobile())
            .ownerId(ownerId)
            .gender(user.getGender())
            .name(user.getName())
            .age(user.getAge())
            .profilePhotoUrl(user.getProfilePhotoUrl() != null && fileExists(user.getProfilePhotoUrl()) ? user.getProfilePhotoUrl() : null)
            .build();
    }

    private boolean fileExists(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) return false;
        try {
            java.nio.file.Path base = java.nio.file.Paths.get(appProperties.getFileStorage().getLocalRoot()).toAbsolutePath().normalize();
            java.nio.file.Path target = base.resolve(storedPath).normalize();
            if (!target.startsWith(base)) return false;
            return java.nio.file.Files.exists(target);
        } catch (Exception e) {
            log.warn("Error checking file existence for {}: {}", storedPath, e.getMessage());
            return false;
        }
    }

    private Integer calculateAge(LocalDate dateOfBirth) {
        return dateOfBirth == null ? null : Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}