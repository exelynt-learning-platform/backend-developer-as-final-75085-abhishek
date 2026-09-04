package com.booking.controller;

import com.booking.dto.request.LoginRequest;
import com.booking.dto.response.AuthResponse;
import com.booking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login and obtain JWT token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(
        summary = "User login",
        description = "Authenticate with username and password. Returns a JWT Bearer token."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
