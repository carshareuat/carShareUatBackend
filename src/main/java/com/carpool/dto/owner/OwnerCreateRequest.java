package com.carpool.dto.owner;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class OwnerCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String mobile;
    private MultipartFile profilePhoto;
    private MultipartFile governmentIdProof;
    private String preferences;
}
