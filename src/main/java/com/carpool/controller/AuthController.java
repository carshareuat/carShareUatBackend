package com.carpool.controller;

import com.carpool.dto.ApiResponse;
import com.carpool.dto.auth.AuthRequest;
import com.carpool.dto.auth.RefreshRequest;
import com.carpool.dto.auth.TokenResponse;
import com.carpool.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<TokenResponse> registerJson(@Valid @RequestBody AuthRequest request,
                                                   jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.of(authService.register(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), null, null));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<TokenResponse> registerMultipart(@Valid @ModelAttribute AuthRequest request,
                                                        jakarta.servlet.http.HttpServletRequest httpRequest,
                                                        @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto,
                                                        @RequestPart(value = "governmentIdProof", required = false) MultipartFile governmentIdProof) {
        return ApiResponse.of(authService.register(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), profilePhoto, governmentIdProof));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody AuthRequest request,
                                            jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.of(authService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request,
                                              jakarta.servlet.http.HttpServletRequest httpRequest) {
        return ApiResponse.of(authService.refresh(request.getRefreshToken(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.of(null);
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.of(authService.me());
    }
}
