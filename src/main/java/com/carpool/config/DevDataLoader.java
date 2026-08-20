package com.carpool.config;

import com.carpool.entity.Role;
import com.carpool.entity.User;
import com.carpool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("local")
@RequiredArgsConstructor
public class DevDataLoader implements org.springframework.boot.CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create a known test passenger user for local development
        final String testMobile = "0000000000";
        final String testPassword = "Password123!";
        Optional<User> existing = userRepository.findByMobile(testMobile);
        if (existing.isPresent()) {
            log.info("Dev user already present: {}", testMobile);
            return;
        }
        User u = new User();
        u.setMobile(testMobile);
        u.setName("dev passenger");
        u.setRole(Role.PASSENGER);
        u.setPasswordHash(passwordEncoder.encode(testPassword));
        userRepository.save(u);
        log.info("Created dev user: mobile={} password={} (local profile only)", testMobile, testPassword);
    }
}
