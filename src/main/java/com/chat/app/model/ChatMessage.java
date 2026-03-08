package com.chat.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

/**
 * Encrypted Message entity for E2EE chat system.
 *
 * SECURITY DESIGN:
 * - Server stores ONLY encrypted data
 * - Server CANNOT decrypt messages (no access to private keys)
 * - encryptedContent: AES-encrypted message content (Base64)
 * - encryptedKey: RSA-encrypted AES session key (Base64)
 *
 * ENCRYPTION FLOW:
 * 1. Sender generates random AES-256 session key
 * 2. Message is encrypted with AES-GCM using session key
 * 3. Session key is encrypted with recipient's RSA public key
 * 4. Both encrypted payloads are sent to server
 * 5. Server stores without decryption
 * 6. Recipient decrypts AES key with their private key
 * 7. Recipient decrypts message with AES key
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username of the message sender
     */
    @Column(nullable = false)
    private String sender;

    /**
     * Username of the message recipient
     */
    @Column(nullable = false)
    private String recipient;

    /**
     * AES-GCM encrypted message content (Base64 encoded)
     * Format: IV + Ciphertext + AuthTag (concatenated, Base64)
     *
     * IMPORTANT: This is NOT plaintext. Server cannot read this.
     */
    @Column(nullable = false, length = 8000)
    private String encryptedContent;

    /**
     * RSA-OAEP encrypted AES session key for RECIPIENT (Base64 encoded)
     * Encrypted with recipient's public key.
     *
     * Only the recipient can decrypt this with their private key.
     */
    @Column(nullable = false, length = 1024)
    private String encryptedKey;

    /**
     * RSA-OAEP encrypted AES session key for SENDER (Base64 encoded)
     * Encrypted with sender's public key.
     *
     * Allows the sender to decrypt their own sent messages.
     */
    @Column(length = 1024)
    private String encryptedKeySender;

    /**
     * Message timestamp (server-side, not encrypted)
     * Note: Metadata like timestamp is visible to server.
     * For maximum privacy, clients could encrypt this too.
     */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
