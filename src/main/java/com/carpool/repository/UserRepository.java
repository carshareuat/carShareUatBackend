package com.carpool.repository;

import com.carpool.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByMobile(String mobile);
    
    Optional<User> findByFirebaseUid(String firebaseUid);
}
