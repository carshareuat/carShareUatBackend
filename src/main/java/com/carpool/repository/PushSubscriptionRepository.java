package com.carpool.repository;

import com.carpool.entity.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {
    List<PushSubscription> findByUserId(UUID userId);
    java.util.Optional<PushSubscription> findByEndpoint(String endpoint);
}
