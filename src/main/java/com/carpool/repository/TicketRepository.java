package com.carpool.repository;

import com.carpool.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
