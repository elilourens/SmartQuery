package com.smartquery.auth;

import com.smartquery.model.User;
import com.smartquery.model.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest body) {
        if (userRepository.existsByEmail(body.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "email already registered"));
        }
        User user = new User(
                UUID.randomUUID().toString(),
                body.email(),
                passwordEncoder.encode(body.password()),
                Instant.now().toString()
        );
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("userId", user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest body) {
        return userRepository.findByEmail(body.email())
                .filter(u -> passwordEncoder.matches(body.password(), u.getPasswordHash()))
                .map(u -> ResponseEntity.ok(Map.of(
                        "accessToken", jwtService.generateAccessToken(u.getId()),
                        "refreshToken", jwtService.generateRefreshToken(u.getId())
                )))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "invalid credentials")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest body) {
        try {
            String userId = jwtService.validateToken(body.refreshToken());
            return ResponseEntity.ok(Map.of(
                    "accessToken", jwtService.generateAccessToken(userId)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "invalid or expired refresh token"));
        }
    }

    record RegisterRequest(String email, String password) {}
    record LoginRequest(String email, String password) {}
    record RefreshRequest(String refreshToken) {}
}
