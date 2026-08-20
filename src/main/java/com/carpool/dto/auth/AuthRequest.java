package com.carpool.dto.auth;

import com.carpool.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AuthRequest {
    @NotNull
    private Role role;

    @NotBlank
    private String mobile;

    private String name;

    @Past
    private LocalDate dateOfBirth;

    private String gender;

    private String firebaseUid;

    private MultipartFile governmentIdProof;
}
