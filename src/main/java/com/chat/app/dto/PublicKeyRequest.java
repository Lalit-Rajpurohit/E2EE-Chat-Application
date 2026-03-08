package com.chat.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for registering/updating user's public key.
 *
 * SECURITY NOTES:
 * - Only the PUBLIC key should be sent to the server
 * - The PRIVATE key must NEVER leave the client device
 * - Public key should be in SPKI format, Base64 encoded
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicKeyRequest {

    /**
     * RSA Public Key in SPKI format, Base64 encoded.
     *
     * Generated on client side using:
     * - Web Crypto API (browser)
     * - Or equivalent crypto library
     *
     * Key specifications:
     * - Algorithm: RSA-OAEP
     * - Modulus length: 2048 bits (minimum)
     * - Hash: SHA-256
     */
    private String publicKey;
}
