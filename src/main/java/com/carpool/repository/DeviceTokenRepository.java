package com.carpool.repository;

import com.carpool.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    // Use the newest row only; older duplicate rows may exist from prior data issues,
    // and this avoids NonUniqueResultException during token registration.
    Optional<DeviceToken> findFirstByUserIdAndFcmTokenOrderByUpdatedAtDesc(UUID userId, String fcmToken);

    List<DeviceToken> findByUserId(UUID userId);

    List<DeviceToken> findByUserIdAndDeviceType(UUID userId, String deviceType);

    boolean existsByUserIdAndFcmToken(UUID userId, String fcmToken);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.userId = :userId AND d.fcmToken = :token")
    void deleteByUserIdAndToken(@Param("userId") UUID userId, @Param("token") String token);

    @Modifying
    @Query("DELETE FROM DeviceToken d WHERE d.userId = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}
