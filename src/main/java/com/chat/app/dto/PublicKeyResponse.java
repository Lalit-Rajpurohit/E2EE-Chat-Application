package com.chat.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for returning a user's public key.
 *
 * Used by sender to encrypt messages for the recipient.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyResponse {

    /**
     * Username of the key owner
     */
    private String username;

    /**
     * RSA Public Key in SPKI format, Base64 encoded.
     * Used to encrypt AES session keys for E2EE messages.
     */
    private String publicKey;
}
