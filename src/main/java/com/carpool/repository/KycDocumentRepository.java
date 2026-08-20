package com.carpool.repository;

import com.carpool.entity.KycDocument;
import com.carpool.entity.KycDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {
    List<KycDocument> findByOwnerId(UUID ownerId);
    boolean existsByOwnerIdAndType(UUID ownerId, KycDocumentType type);
    boolean existsByUserIdAndType(UUID userId, KycDocumentType type);
}
