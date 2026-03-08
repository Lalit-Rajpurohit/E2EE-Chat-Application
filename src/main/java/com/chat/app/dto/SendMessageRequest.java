package com.chat.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for sending E2EE encrypted messages.
 *
 * SECURITY: All encryption happens on the CLIENT side.
 * Server receives and stores ONLY encrypted data.
 *
 * CLIENT-SIDE ENCRYPTION STEPS:
 * 1. Generate random AES-256 key (session key)
 * 2. Encrypt plaintext message with AES-GCM → encryptedMessage
 * 3. Fetch recipient's RSA public key from server
 * 4. Encrypt AES key with RSA-OAEP → encryptedKey
 * 5. Send this DTO to server
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {

    /**
     * Username of the sender
     */
    private String senderId;

    /**
     * Username of the recipient
     */
    private String receiverId;

    /**
     * Base64-encoded AES-GCM encrypted message.
     * Format: Base64(IV || Ciphertext || AuthTag)
     */
    private String encryptedMessage;

    /**
     * Base64-encoded RSA-OAEP encrypted AES session key for recipient.
     * Encrypted with recipient's public key.
     */
    private String encryptedKey;

    /**
     * Base64-encoded RSA-OAEP encrypted AES session key for sender.
     * Encrypted with sender's own public key.
     * Allows sender to decrypt their own sent messages.
     */
    private String encryptedKeySender;
}
