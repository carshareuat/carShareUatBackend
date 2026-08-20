package com.carpool.service;

import com.carpool.dto.ApiResponse;
import com.carpool.entity.Ticket;
import com.carpool.entity.TicketCategory;
import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.TicketCategoryRepository;
import com.carpool.repository.TicketRepository;
import com.carpool.security.AuthFacade;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final AuthFacade authFacade;
    private final NotificationService notificationService;
    private final com.carpool.repository.UserRepository userRepository;

    public List<TicketCategory> categories() { return categoryRepository.findAll(); }

    @Transactional
    public Ticket create(MultipartFile image, String category, String description) {
        java.util.UUID userId = authFacade.currentUser().getUserId();
        User u = userRepository.findById(userId).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        Ticket t = new Ticket();
        t.setUser(u);
        t.setCategory(category);
        t.setDescription(description);
        if (image != null && !image.isEmpty()) {
            try {
                Path dir = Path.of("storage/tickets"); Files.createDirectories(dir);
                String filename = System.currentTimeMillis() + "_" + image.getOriginalFilename();
                Path dest = dir.resolve(filename);
                Files.copy(image.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
                t.setImagePath("/files/tickets/" + filename);
            } catch (IOException e) {
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "UPLOAD_FAILED", "Unable to store image");
            }
        }
        Ticket saved = ticketRepository.save(t);
        // notify submitter
        notificationService.create(u.getId(), com.carpool.entity.NotificationType.TICKET_RAISED, "Ticket raised", "Ticket #" + saved.getId() + " created.");
        // notify admins
        try {
            userRepository.findAll().stream().filter(us -> us.getRole() == com.carpool.entity.Role.ADMIN).forEach(admin -> {
                try { notificationService.create(admin.getId(), com.carpool.entity.NotificationType.TICKET_RAISED, "Ticket raised", "Ticket #" + saved.getId() + " created by " + u.getMobile()); } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        return saved;
    }

    public List<Ticket> myTickets() {
        java.util.UUID userId = authFacade.currentUser().getUserId();
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Ticket> adminList(String status) {
        if (authFacade.currentUser().getRole() != com.carpool.entity.Role.ADMIN) throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Admin only");
        if (status == null || status.isBlank()) return ticketRepository.findAll();
        return ticketRepository.findAll().stream().filter(t -> t.getStatus().equalsIgnoreCase(status)).toList();
    }

    public Ticket find(UUID id) { return ticketRepository.findById(id).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ticket not found")); }

    @Transactional
    public Ticket resolve(UUID id, String resolution) {
        Ticket t = find(id);
        t.setResolution(resolution);
        t.setStatus("RESOLVED");
        ticketRepository.save(t);
        notificationService.create(t.getUser().getId(), com.carpool.entity.NotificationType.TICKET_RESOLVED, "Ticket resolved", "Your ticket #" + t.getId() + " is resolved.");
        return t;
    }
}
