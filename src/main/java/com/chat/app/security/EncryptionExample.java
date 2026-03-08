package com.chat.app.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * =====================================================================
 * ENCRYPTION EXAMPLE - FOR CLIENT-SIDE REFERENCE ONLY
 * =====================================================================
 *
 * IMPORTANT: This class demonstrates the encryption algorithms
 * that should be implemented on the CLIENT SIDE (browser/mobile app).
 *
 * THE SERVER MUST NEVER:
 * - Decrypt messages
 * - Store private keys
 * - Have access to plaintext content
 *
 * This code is provided as a reference for:
 * - Understanding the encryption flow
 * - Implementing equivalent logic in JavaScript/mobile
 * - Testing purposes only
 *
 * =====================================================================
 * E2EE ENCRYPTION FLOW
 * =====================================================================
 *
 * REGISTRATION:
 * 1. Client generates RSA-2048 key pair
 * 2. Public key → sent to server (stored in User.publicKey)
 * 3. Private key → stored ONLY on client (localStorage/secure storage)
 *
 * SENDING MESSAGE:
 * 1. Sender fetches recipient's public key from server
 * 2. Sender generates random AES-256 session key
 * 3. Message encrypted with AES-GCM (session key)
 * 4. Session key encrypted with recipient's RSA public key
 * 5. Both encrypted blobs sent to server
 *
 * RECEIVING MESSAGE:
 * 1. Client receives encrypted message + encrypted key
 * 2. Decrypt AES key using own RSA private key
 * 3. Decrypt message using AES key
 * 4. Display plaintext to user
 *
 * =====================================================================
 */
public class EncryptionExample {

    // AES Configuration
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    // RSA Configuration
    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * Generate RSA Key Pair (CLIENT-SIDE OPERATION)
     *
     * Call this during user registration.
     * Store private key securely on client.
     * Send public key to server.
     */
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE, new SecureRandom());
        return generator.generateKeyPair();
    }

    /**
     * Export public key to Base64 string (for sending to server)
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * Export private key to Base64 string (for secure client storage)
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * Import public key from Base64 string
     */
    public static PublicKey base64ToPublicKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }

    /**
     * Import private key from Base64 string
     */
    public static PrivateKey base64ToPrivateKey(String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    /**
     * Generate random AES session key (CLIENT-SIDE OPERATION)
     *
     * A new key should be generated for EACH message
     * for forward secrecy.
     */
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(AES_KEY_SIZE, new SecureRandom());
        return generator.generateKey();
    }

    /**
     * Encrypt message with AES-GCM (CLIENT-SIDE OPERATION)
     *
     * Returns: Base64(IV || Ciphertext || AuthTag)
     */
    public static String encryptWithAES(String plaintext, SecretKey aesKey) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, spec);

        // Encrypt
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV + Ciphertext (GCM includes auth tag in ciphertext)
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * Decrypt message with AES-GCM (CLIENT-SIDE OPERATION)
     *
     * Input: Base64(IV || Ciphertext || AuthTag)
     */
    public static String decryptWithAES(String encryptedBase64, SecretKey aesKey) throws Exception {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

        // Extract ciphertext
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // Initialize cipher
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, spec);

        // Decrypt
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, "UTF-8");
    }

    /**
     * Encrypt AES key with RSA public key (CLIENT-SIDE OPERATION)
     *
     * Used to securely transmit the AES session key to recipient.
     */
    public static String encryptAESKeyWithRSA(SecretKey aesKey, PublicKey rsaPublicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    /**
     * Decrypt AES key with RSA private key (CLIENT-SIDE OPERATION)
     *
     * Used by recipient to recover the AES session key.
     */
    public static SecretKey decryptAESKeyWithRSA(String encryptedKeyBase64, PrivateKey rsaPrivateKey) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(encryptedKeyBase64);
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
        byte[] aesKeyBytes = cipher.doFinal(encryptedKey);
        return new SecretKeySpec(aesKeyBytes, "AES");
    }

    /**
     * =====================================================================
     * COMPLETE E2EE MESSAGE FLOW EXAMPLE
     * =====================================================================
     *
     * This demonstrates the full encryption/decryption cycle.
     * In production, this happens entirely on the CLIENT.
     */
    public static void main(String[] args) {
        try {
            System.out.println("=== E2EE Demo ===\n");

            // STEP 1: User registration (both users generate key pairs)
            System.out.println("1. Generating key pairs for Alice and Bob...");
            KeyPair aliceKeyPair = generateRSAKeyPair();
            KeyPair bobKeyPair = generateRSAKeyPair();

            String alicePublicKeyB64 = publicKeyToBase64(aliceKeyPair.getPublic());
            String bobPublicKeyB64 = publicKeyToBase64(bobKeyPair.getPublic());

            System.out.println("   Alice's public key: " + alicePublicKeyB64.substring(0, 50) + "...");
            System.out.println("   Bob's public key: " + bobPublicKeyB64.substring(0, 50) + "...\n");

            // STEP 2: Alice sends message to Bob
            String originalMessage = "Hello Bob! This is a secret message.";
            System.out.println("2. Alice's original message: \"" + originalMessage + "\"\n");

            // Alice fetches Bob's public key (from server)
            PublicKey bobPublicKey = base64ToPublicKey(bobPublicKeyB64);

            // Alice generates AES session key
            SecretKey sessionKey = generateAESKey();

            // Alice encrypts message with AES
            String encryptedMessage = encryptWithAES(originalMessage, sessionKey);
            System.out.println("3. Encrypted message (AES-GCM): " + encryptedMessage.substring(0, 50) + "...\n");

            // Alice encrypts AES key with Bob's public key
            String encryptedKey = encryptAESKeyWithRSA(sessionKey, bobPublicKey);
            System.out.println("4. Encrypted AES key (RSA-OAEP): " + encryptedKey.substring(0, 50) + "...\n");

            // STEP 3: Server stores encrypted data (cannot decrypt!)
            System.out.println("5. Server stores: encryptedMessage + encryptedKey");
            System.out.println("   Server CANNOT read the message content!\n");

            // STEP 4: Bob receives and decrypts
            System.out.println("6. Bob receives encrypted message...");

            // Bob decrypts AES key with his private key
            SecretKey decryptedSessionKey = decryptAESKeyWithRSA(encryptedKey, bobKeyPair.getPrivate());

            // Bob decrypts message with AES key
            String decryptedMessage = decryptWithAES(encryptedMessage, decryptedSessionKey);

            System.out.println("7. Bob's decrypted message: \"" + decryptedMessage + "\"\n");

            System.out.println("=== E2EE Success! ===");
            System.out.println("Message was encrypted on sender's device,");
            System.out.println("transmitted securely, and decrypted only on recipient's device.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
