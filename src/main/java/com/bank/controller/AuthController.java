package com.bank.controller;

import com.bank.dto.AuthResponse;
import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;
import com.bank.dto.UserDTO;
import com.bank.domain.User;
import com.bank.security.JwtUtil;
import com.bank.service.EmailService;
import com.bank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication endpoints for login and registration")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.mapToDTO(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        try {
            com.bank.dto.UserDTO userDTO = userService.getUserByUsername(userDetails.getUsername());
            emailService.sendLoginAlert(userDTO.getEmail(), userDTO.getFullName(), "unknown");
        } catch (Exception ignored) {}

        return ResponseEntity.ok(new AuthResponse(token, userDetails.getUsername(), role, jwtUtil.getExpiration()));
    }
}
