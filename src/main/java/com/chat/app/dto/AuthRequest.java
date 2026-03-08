package com.chat.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO for authentication requests (login/register).
 *
 * For E2EE registration, the publicKey field is used to store
 * the user's RSA public key during account creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    private String username;

    private String password;

    /**
     * RSA Public Key (Base64 encoded SPKI format)
     * Required during registration for E2EE support.
     * The private key stays on the client device.
     */
    private String publicKey;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
