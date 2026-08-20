package com.carpool.repository;

import com.carpool.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    Optional<SubscriptionPlan> findFirstByActiveTrueOrderByCreatedAtAsc();
    java.util.List<SubscriptionPlan> findByActiveTrueOrderByCreatedAtAsc();
    Optional<SubscriptionPlan> findByCode(String code);
}