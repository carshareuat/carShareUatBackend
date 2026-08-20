package com.carpool.controller;

import com.carpool.security.AppUserPrincipal;
import com.carpool.security.AuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
public class DebugController {
    private final AuthFacade authFacade;

    @GetMapping("/whoami")
    public Map<String, Object> whoami() {
        AppUserPrincipal p = authFacade.currentUser();
        return Map.of(
            "userId", p.getUserId(),
            "ownerId", p.getOwnerId(),
            "mobile", p.getMobile(),
            "role", p.getRole()
        );
    }
}
