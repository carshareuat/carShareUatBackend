package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.entity.Ticket;
import com.carpool.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/categories")
    public ApiResponse<List<?>> categories() { return ApiResponse.of(ticketService.categories()); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> create(@RequestPart(required = false) MultipartFile image, @RequestPart String category, @RequestPart String description) {
        return ApiResponse.of(ticketService.create(image, category, description));
    }

    @GetMapping("/me")
    public ApiResponse<List<Ticket>> myTickets() { return ApiResponse.of(ticketService.myTickets()); }

    @GetMapping("/admin")
    public ApiResponse<List<Ticket>> adminList(@RequestParam(required = false) String status) { return ApiResponse.of(ticketService.adminList(status)); }

    @GetMapping("/{id}")
    public ApiResponse<Ticket> get(@PathVariable UUID id) { return ApiResponse.of(ticketService.find(id)); }

    @PostMapping("/{id}/resolve")
    public ApiResponse<?> resolve(@PathVariable UUID id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String resolution = body == null ? "" : body.getOrDefault("resolution", "");
        return ApiResponse.of(ticketService.resolve(id, resolution));
    }
}
