package com.carpool.security;

import com.carpool.entity.User;
import com.carpool.exception.AppException;
import com.carpool.repository.OwnerProfileRepository;
import com.carpool.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByMobile(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return toPrincipal(user);
    }

    public AppUserPrincipal loadPrincipalById(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "User not found"));
        return toPrincipal(user);
    }

    private AppUserPrincipal toPrincipal(User user) {
        Optional<UUID> ownerId = ownerProfileRepository.findByUserId(user.getId()).map(o -> o.getId());
        return AppUserPrincipal.builder()
            .userId(user.getId())
            .ownerId(ownerId.orElse(null))
            .mobile(user.getMobile())
            .role(user.getRole())
            .build();
    }
}
