package com.chat.app.controller;

import com.chat.app.dto.SendMessageRequest;
import com.chat.app.model.ChatMessage;
import com.chat.app.repository.ChatMessageRepository;
import com.chat.app.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * Chat Controller for E2EE Messaging System.
 *
 * =====================================================================
 * CRITICAL SECURITY DESIGN
 * =====================================================================
 *
 * This controller handles ONLY encrypted payloads:
 * - encryptedContent: AES-GCM encrypted message (Base64)
 * - encryptedKey: RSA-OAEP encrypted AES key (Base64)
 *
 * THE SERVER NEVER:
 * - Decrypts messages
 * - Has access to plaintext
 * - Stores private keys
 *
 * THE SERVER ONLY:
 * - Receives encrypted data from sender
 * - Stores encrypted data in database
 * - Forwards encrypted data to recipient
 *
 * All encryption/decryption happens on CLIENT devices.
 * =====================================================================
 */
@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository messageRepository;
    private final JwtUtil jwtUtil;

    public ChatController(SimpMessagingTemplate messagingTemplate,
                          ChatMessageRepository messageRepository,
                          JwtUtil jwtUtil) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * WebSocket endpoint for sending E2EE encrypted messages.
     *
     * E2EE FLOW (happens on CLIENT before calling this):
     * 1. Sender generates random AES-256 session key
     * 2. Sender encrypts plaintext with AES-GCM → encryptedContent
     * 3. Sender fetches recipient's RSA public key
     * 4. Sender encrypts AES key with RSA-OAEP → encryptedKey
     * 5. Sender calls this endpoint with encrypted payload
     *
     * SERVER ACTIONS (this method):
     * 1. Receive encrypted payload (cannot read content!)
     * 2. Store encrypted data in database
     * 3. Forward encrypted data to recipient via WebSocket
     * 4. Forward to sender for confirmation
     *
     * RECIPIENT DECRYPTION (happens on CLIENT after receiving):
     * 1. Decrypt AES key using own RSA private key
     * 2. Decrypt message content using AES key
     * 3. Display plaintext to user
     */
    @MessageMapping("/sendMessage")
    public void sendMessage(@Payload SendMessageRequest request) {
        // Create encrypted message entity
        // NOTE: Server stores ONLY encrypted data - cannot decrypt!
        ChatMessage message = new ChatMessage();
        message.setSender(request.getSenderId());
        message.setRecipient(request.getReceiverId());
        message.setEncryptedContent(request.getEncryptedMessage());
        message.setEncryptedKey(request.getEncryptedKey());
        message.setEncryptedKeySender(request.getEncryptedKeySender());

        // Save encrypted message to database
        messageRepository.save(message);

        // Forward encrypted message to recipient's topic
        // Recipient will decrypt on their device
        messagingTemplate.convertAndSend(
                "/topic/messages/" + request.getReceiverId(),
                message
        );

        // Forward to sender's topic for confirmation
        messagingTemplate.convertAndSend(
                "/topic/messages/" + request.getSenderId(),
                message
        );
    }

    /**
     * REST endpoint to fetch conversation history (encrypted).
     *
     * Returns encrypted messages that client will decrypt locally.
     *
     * IMPORTANT: Messages returned are ENCRYPTED.
     * Client must decrypt each message using their private key.
     */
    @GetMapping("/api/messages")
    @ResponseBody
    public ResponseEntity<List<ChatMessage>> getConversation(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String withUser) {

        String token = authHeader.substring(7);
        String currentUser = jwtUtil.extractUsername(token);

        // Return encrypted messages
        // Client will decrypt them locally
        List<ChatMessage> messages = messageRepository.findConversation(currentUser, withUser);
        return ResponseEntity.ok(messages);
    }

    // =====================================================================
    // VIEW ENDPOINTS (unchanged)
    // =====================================================================

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
