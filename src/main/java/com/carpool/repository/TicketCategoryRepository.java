package com.carpool.repository;

import com.carpool.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketCategoryRepository extends JpaRepository<TicketCategory, UUID> {
}
