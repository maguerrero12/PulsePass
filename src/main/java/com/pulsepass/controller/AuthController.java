package com.pulsepass.controller;

import com.pulsepass.domain.User;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthorizationService authorizationService;

    public AuthController(AuthService authService, AuthorizationService authorizationService) {
        this.authService = authService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> me() {

        SecurityPrincipal principal =
                authorizationService.getCurrentPrincipal();

        User user = principal.getUser();
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        AuthenticatedUserResponse response = new AuthenticatedUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getActive()),
                roles
        );

        return ResponseEntity.ok(response);
    }
}
