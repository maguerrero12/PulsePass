package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthorizationService authorizationService;

    public UserController(AuthService authService, UserService userService, AuthorizationService authorizationService) {
        this.authService = authService;
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        authorizationService.verifyUserAccessByEmail(email);
        return ResponseEntity.ok(userService.findByEmail(email));
    }

    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username) {
        authorizationService.verifyUserAccessByUsername(username);
        return ResponseEntity.ok(userService.findByUsername(username));
    }
}
