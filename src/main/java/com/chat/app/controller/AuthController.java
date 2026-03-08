package com.chat.app.controller;

import com.chat.app.dto.AuthRequest;
import com.chat.app.dto.AuthResponse;
import com.chat.app.dto.PublicKeyRequest;
import com.chat.app.dto.PublicKeyResponse;
import com.chat.app.model.User;
import com.chat.app.repository.UserRepository;
import com.chat.app.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Authentication Controller for E2EE Chat System.
 *
 * SECURITY RESPONSIBILITIES:
 * - User registration with public key storage
 * - Public key retrieval for E2EE encryption
 * - NEVER handles private keys (they stay on client)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Register new user with E2EE public key.
     *
     * E2EE FLOW:
     * 1. Client generates RSA key pair locally
     * 2. Client sends username, password, and PUBLIC key
     * 3. Server stores public key for other users to fetch
     * 4. PRIVATE key remains on client device only
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest()
                    .body(new AuthResponse(null, null, "Username already exists"));
        }

        // Create user with public key for E2EE
        User user = new User(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getPublicKey()  // Store public key for E2EE
        );
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token, user.getUsername()));
    }

    /**
     * Login existing user.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            user.setOnline(true);
            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, user.getUsername()));

        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse(null, null, "Invalid credentials"));
        }
    }

    /**
     * Logout user.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            userRepository.findByUsername(username).ifPresent(user -> {
                user.setOnline(false);
                userRepository.save(user);
            });
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    /**
     * Get user's public key by username.
     *
     * E2EE USAGE:
     * - Sender fetches recipient's public key
     * - Uses it to encrypt AES session key
     * - Only recipient can decrypt with their private key
     *
     * SECURITY NOTE:
     * This endpoint returns ONLY the public key.
     * Private keys are NEVER transmitted or stored on server.
     */
    @GetMapping("/users/{username}/public-key")
    public ResponseEntity<PublicKeyResponse> getPublicKey(@PathVariable String username) {
        return userRepository.findByUsername(username)
                .map(user -> ResponseEntity.ok(new PublicKeyResponse(user.getUsername(), user.getPublicKey())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update user's public key.
     *
     * USE CASE: Key rotation or device change.
     * Client generates new key pair and sends new public key.
     */
    @PutMapping("/users/public-key")
    public ResponseEntity<String> updatePublicKey(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody PublicKeyRequest request) {

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        return userRepository.findByUsername(username)
                .map(user -> {
                    user.setPublicKey(request.getPublicKey());
                    userRepository.save(user);
                    return ResponseEntity.ok("Public key updated successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get online users.
     */
    @GetMapping("/users/online")
    public ResponseEntity<List<String>> getOnlineUsers() {
        List<String> onlineUsers = userRepository.findByOnlineTrue()
                .stream()
                .map(User::getUsername)
                .toList();
        return ResponseEntity.ok(onlineUsers);
    }

    /**
     * Get all users except current user.
     */
    @GetMapping("/users")
    public ResponseEntity<List<String>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        String currentUser = jwtUtil.extractUsername(token);

        List<String> users = userRepository.findAll()
                .stream()
                .map(User::getUsername)
                .filter(username -> !username.equals(currentUser))
                .toList();
        return ResponseEntity.ok(users);
    }
}
