package com.carpool.dto.auth;

import com.carpool.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class MeResponse {
    private UUID userId;
    private Role role;
    private String mobile;
    private UUID ownerId;
    private String gender;
    private String name;
    private Integer age;
    private String profilePhotoUrl;
}
