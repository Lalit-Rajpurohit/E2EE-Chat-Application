package com.chat.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * User entity for E2EE chat system.
 *
 * SECURITY NOTE:
 * - publicKey: Stores the user's RSA public key (Base64 encoded)
 * - The private key is NEVER stored on the server
 * - Private key remains exclusively on the client device
 * - Public key is used by other users to encrypt messages TO this user
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    /**
     * RSA Public Key (Base64 encoded SPKI format)
     * Used for encrypting AES session keys during E2EE message exchange.
     *
     * Flow:
     * 1. Client generates RSA key pair locally
     * 2. Public key is sent to server during registration/key update
     * 3. Private key NEVER leaves the client
     * 4. Other users fetch this public key to encrypt messages
     */
    @Column(length = 2048)
    private String publicKey;

    @Column(nullable = false)
    private boolean online = false;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String publicKey) {
        this.username = username;
        this.password = password;
        this.publicKey = publicKey;
    }
}
