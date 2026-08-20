package com.carpool.service;

import com.carpool.dto.owner.OwnerCreateRequest;
import com.carpool.dto.owner.OwnerResponse;
import com.carpool.entity.KycDocument;
import com.carpool.entity.KycDocumentType;
import com.carpool.entity.OwnerProfile;
import com.carpool.entity.Role;
import com.carpool.entity.VerificationStatus;
import com.carpool.exception.AppException;
import com.carpool.mapper.OwnerMapper;
import com.carpool.repository.KycDocumentRepository;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.UserRepository;
import com.carpool.security.AuthFacade;
import com.carpool.security.AppUserPrincipal;
import com.carpool.validation.MobileNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OwnerService {

    private final OwnerProfileRepository ownerRepository;
    private final UserRepository userRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final FileStorageService fileStorageService;
    private final MobileNormalizer mobileNormalizer;
    private final AuthFacade authFacade;
    private final OwnerMapper ownerMapper;
    private final NotificationService notificationService;
    private final Environment env;

    @Transactional
    public OwnerResponse create(OwnerCreateRequest request) {
        AppUserPrincipal principal = authFacade.currentUser();
        if (principal.getRole() != Role.OWNER && principal.getRole() != Role.ADMIN) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only owner/admin can create owner profile");
        }
        if (ownerRepository.findByUserId(principal.getUserId()).isPresent()) {
            throw new AppException(HttpStatus.CONFLICT, "DUPLICATE_OWNER", "Owner profile already exists");
        }

        OwnerProfile owner = new OwnerProfile();
        owner.setUser(userRepository.findById(principal.getUserId()).orElseThrow());
        owner.setName(request.getName());
        owner.setMobile(mobileNormalizer.normalize(request.getMobile()));
        owner.setPreferences(request.getPreferences());
        owner.setVerificationStatus(VerificationStatus.PENDING);
        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isEmpty()) {
            owner.setProfilePhotoUrl(fileStorageService.storePublicProfile(request.getProfilePhoto()));
        }
        owner = ownerRepository.save(owner);
        // Notify owner to subscribe for a subscription to enable posting rides and receiving bookings
        try {
            notificationService.create(owner.getUser().getId(), com.carpool.entity.NotificationType.SUBSCRIPTION_PENDING,
                "Subscription pending", "Complete subscription payment to start posting rides and receiving bookings.");
        } catch (Exception ignored) {}

        if (request.getGovernmentIdProof() != null && !request.getGovernmentIdProof().isEmpty()) {
            KycDocument doc = new KycDocument();
            doc.setOwner(owner);
            doc.setType(KycDocumentType.GOVERNMENT_ID);
            doc.setPrivateFile(true);
            doc.setMimeType(request.getGovernmentIdProof().getContentType());
            doc.setSizeBytes(request.getGovernmentIdProof().getSize());
            doc.setStoragePath(fileStorageService.storePrivateKyc(request.getGovernmentIdProof()));
            kycDocumentRepository.save(doc);
        }
        return ownerMapper.toResponse(owner);
    }

    public List<OwnerResponse> list() {
        return ownerRepository.findAll().stream().map(ownerMapper::toResponse).toList();
    }

    public OwnerResponse get(UUID ownerId) {
        return ownerMapper.toResponse(ownerRepository.findById(ownerId)
            .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Owner not found")));
    }
}
