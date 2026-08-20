package com.carpool.config;

import com.carpool.entity.Role;
import com.carpool.entity.SubscriptionPlan;
import com.carpool.entity.User;
import com.carpool.repository.SubscriptionPlanRepository;
import com.carpool.repository.UserRepository;
import com.carpool.validation.MobileNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BootstrapDataInitializer implements CommandLineRunner {
    private final AppProperties properties;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final MobileNormalizer mobileNormalizer;
    private final com.carpool.repository.TicketCategoryRepository ticketCategoryRepository;

    @Override
    @Transactional
    public void run(String... args) {
        String adminMobile = mobileNormalizer.normalize(properties.getAdmin().getMobile());
        // Ensure admin user exists. If created here, set hashed password. If already exists but has no password_hash, set it.
        User adminUser = userRepository.findByMobile(adminMobile).orElseGet(() -> {
            User admin = new User();
            admin.setRole(Role.ADMIN);
            admin.setMobile(adminMobile);
            admin.setPasswordHash(passwordEncoder.encode(properties.getAdmin().getPassword()));
            return userRepository.save(admin);
        });

        if (adminUser.getPasswordHash() == null || adminUser.getPasswordHash().isBlank()) {
            adminUser.setPasswordHash(passwordEncoder.encode(properties.getAdmin().getPassword()));
            userRepository.save(adminUser);
        }

        // Ensure starter and pro plans exist (idempotent)
        if (planRepository.findByCode("OWNER_STARTER").isEmpty()) {
            SubscriptionPlan starter = new SubscriptionPlan();
            starter.setCode("OWNER_STARTER");
            starter.setName("Starter plan");
            starter.setAmountPaise(29900);
            starter.setCurrency("INR");
            starter.setDurationMonths(3);
            starter.setActive(true);
            starter.setDescription("24x7 support; Owner verification badge; Valid for 3 months");
            planRepository.save(starter);
        }

        if (planRepository.findByCode("OWNER_PRO").isEmpty()) {
            SubscriptionPlan pro = new SubscriptionPlan();
            pro.setCode("OWNER_PRO");
            pro.setName("Pro plan");
            pro.setAmountPaise(59900);
            pro.setCurrency("INR");
            pro.setDurationMonths(6);
            pro.setActive(true);
            pro.setDescription("Priority rides; 24x7 support; Owner verification badge; Valid for 6 months");
            planRepository.save(pro);
        }

        if (ticketCategoryRepository.count() == 0) {
            var c1 = new com.carpool.entity.TicketCategory(); c1.setCode("APPLICATION_ISSUE"); c1.setLabel("Application issue"); c1.setForRole("ALL");
            var c2 = new com.carpool.entity.TicketCategory(); c2.setCode("RIDE_ISSUE"); c2.setLabel("Ride issue"); c2.setForRole("ALL");
            var c3 = new com.carpool.entity.TicketCategory(); c3.setCode("OWNER_ISSUE"); c3.setLabel("Owner issue"); c3.setForRole("PASSENGER");
            var c4 = new com.carpool.entity.TicketCategory(); c4.setCode("PASSENGER_ISSUE"); c4.setLabel("Passenger issue"); c4.setForRole("OWNER");
            ticketCategoryRepository.saveAll(java.util.List.of(c1,c2,c3,c4));
        }
    }
}