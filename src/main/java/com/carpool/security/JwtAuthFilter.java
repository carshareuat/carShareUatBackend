package com.carpool.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import com.carpool.entity.Role;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = authHeader.substring(7);
            Claims claims = jwtService.parse(token);
            UUID userId = UUID.fromString(claims.getSubject());
            // Load base principal from DB, but override role/ownerId from token claims when present.
            AppUserPrincipal base = userDetailsService.loadPrincipalById(userId);
            String roleClaim = claims.get("role", String.class);
            String ownerIdClaim = claims.get("ownerId", String.class);
            UUID ownerId = null;
            if (ownerIdClaim != null && !ownerIdClaim.isBlank()) {
                try { ownerId = UUID.fromString(ownerIdClaim); } catch (Exception ignored) {}
            }
            Role role = base.getRole();
            if (roleClaim != null && !roleClaim.isBlank()) {
                try { role = Role.valueOf(roleClaim); } catch (Exception ignored) {}
            }
            log.debug("JwtAuthFilter: request={} userId={} tokenRole={} tokenOwnerId={} dbRole={} dbOwnerId={} resolvedRole={} resolvedOwnerId={}",
                request.getRequestURI(), userId, roleClaim, ownerIdClaim, base.getRole(), base.getOwnerId(), role, ownerId != null ? ownerId : base.getOwnerId());
            AppUserPrincipal principal = AppUserPrincipal.builder()
                .userId(base.getUserId())
                .ownerId(ownerId != null ? ownerId : base.getOwnerId())
                .mobile(base.getMobile())
                .role(role)
                .build();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
