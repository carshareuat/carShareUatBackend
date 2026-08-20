package com.carpool.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.Locale;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "char(36)")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, unique = true, length = 20)
    private String mobile;

    @Column(length = 120)
    private String name;

    @Column(length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private boolean otpVerified = false;

    @Column(columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime otpVerifiedOn;

    @Column(columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime lastLogin;

    @Column(columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime lastOtpRequest;

    @Column(nullable = false)
    private int failedOtpAttempts = 0;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private Integer age;

    @Column(length = 10)
    private String gender;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 500)
    private String profilePhotoUrl;

    @Column(nullable = false)
    private boolean mobileVerified = false;

    @Column(name = "is_kyc_verified", nullable = false)
    private boolean kycVerified = false;

    public String getFullName() {
        return name != null && !name.isBlank() ? name : mobile;
    }

    public void setFullName(String fullName) {
        this.name = fullName;
    }

    @Column(columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime verifiedOn;

    @Column(length = 255, unique = true)
    private String firebaseUid;

    @PrePersist
    void normalize() {
        if (mobile != null) {
            mobile = mobile.trim().replace(" ", "").toLowerCase(Locale.ROOT);
        }
    }
}
