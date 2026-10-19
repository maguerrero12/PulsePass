package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pulsepass.domain.User;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthorizationService;

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
    public ResponseEntity<UserResponse> me() {

        SecurityPrincipal principal =
                authorizationService.getCurrentPrincipal();

        User user = principal.getUser();

        UserResponse response = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getActive())
        );

        return ResponseEntity.ok(response);
    }
}