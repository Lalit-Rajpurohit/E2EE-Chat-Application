# E2EE Chat Application

![End-to-End Encrypted Chat](coverimage.png)

A secure real-time chat application with **End-to-End Encryption (E2EE)** built with Spring Boot and WebSocket. Messages are encrypted on the sender's device and can only be decrypted by the intended recipient. The server never has access to plaintext messages.

## Features

- **End-to-End Encryption** - Messages encrypted client-side using AES-256-GCM
- **RSA Key Exchange** - 2048-bit RSA keys for secure AES key transmission
- **Real-time Messaging** - WebSocket (STOMP) for instant message delivery
- **JWT Authentication** - Secure user authentication with JSON Web Tokens
- **SQLite Database** - Lightweight database for message and user storage
- **Modern UI** - Clean, responsive interface

## How E2EE Works

### Key Generation (Registration)
```
User registers
    │
    ▼
Browser generates RSA-2048 key pair
    │
    ├── Public Key  ──────► Sent to server (stored in database)
    │
    └── Private Key ──────► Stored in User Device
```

### Sending a Message
```
Alice wants to send "Hello Bob!" to Bob
    │
    ▼
1. Generate random AES-256 session key
    │
    ▼
2. Encrypt message with AES-GCM
   "Hello Bob!" → "xK9#mP2$nQ..." (ciphertext)
    │
    ▼
3. Fetch Bob's public key from server
    │
    ▼
4. Encrypt AES key with Bob's RSA public key
    │
    ▼
5. Encrypt AES key with Alice's RSA public key (for sent messages)
    │
    ▼
6. Send to server: { encryptedMessage, encryptedKey, encryptedKeySender }
```

### Server Role
```
┌─────────────────────────────────────────┐
│              Spring Boot Server          │
├─────────────────────────────────────────┤
│  • Receives encrypted payload            │
│  • Stores encrypted data in database     │
│  • Forwards encrypted data via WebSocket │
│  • CANNOT decrypt messages               │
│  • CANNOT read plaintext                 │
│  • Has NO access to private keys         │
└─────────────────────────────────────────┘
```

### Receiving a Message
```
Bob receives encrypted message
    │
    ▼
1. Decrypt AES key using Bob's private key (from localStorage)
    │
    ▼
2. Decrypt message using AES key
   "xK9#mP2$nQ..." → "Hello Bob!"
    │
    ▼
3. Display plaintext to Bob
```

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 4.0.3 |
| Security | Spring Security + JWT |
| Database | SQLite |
| Real-time | WebSocket (STOMP + SockJS) |
| Frontend | HTML5, CSS3, JavaScript |
| Encryption | Web Crypto API (RSA-OAEP, AES-GCM) |

## Project Structure

```
src/main/java/com/chat/app/
├── config/
│   ├── SecurityConfig.java        # Spring Security + JWT configuration
│   └── WebSocketConfig.java       # WebSocket STOMP configuration
├── controller/
│   ├── AuthController.java        # Login, Register, Public Key endpoints
│   └── ChatController.java        # Message handling (encrypted only)
├── dto/
│   ├── AuthRequest.java           # Login/Register request
│   ├── AuthResponse.java          # JWT token response
│   ├── PublicKeyRequest.java      # Public key update
│   ├── PublicKeyResponse.java     # Public key retrieval
│   └── SendMessageRequest.java    # Encrypted message payload
├── model/
│   ├── ChatMessage.java           # Encrypted message entity
│   └── User.java                  # User entity with public key
├── repository/
│   ├── ChatMessageRepository.java
│   └── UserRepository.java
└── security/
    ├── CustomUserDetailsService.java
    ├── EncryptionExample.java     # Reference encryption implementation
    ├── JwtAuthenticationFilter.java
    └── JwtUtil.java
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user with public key |
| POST | `/api/auth/login` | Authenticate and get JWT token |
| POST | `/api/auth/logout` | Logout user |
| GET | `/api/auth/users` | Get all users |
| GET | `/api/auth/users/{username}/public-key` | Get user's public key |
| PUT | `/api/auth/users/public-key` | Update own public key |
| GET | `/api/messages?withUser={username}` | Get encrypted conversation |
| WS | `/app/sendMessage` | Send encrypted message (WebSocket) |

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Modern web browser (Chrome, Firefox, Edge)

## How to Run

### 1. Clone the repository
```bash
git clone <repository-url>
cd app
```

### 2. Build the project
```bash
mvn clean install
```

### 3. Run the application
```bash
mvn spring-boot:run
```

### 4. Open in browser
```
http://localhost:8080
```

### 5. Test E2EE messaging

1. **Register User 1** - Open browser tab, go to `/register`, create account
2. **Register User 2** - Open another browser/incognito tab, create second account
3. **Start chatting** - Select contact and send encrypted messages
4. **Verify encryption** - Check database to confirm messages are encrypted

## Database

The application uses SQLite. Database file is created automatically:
```
e2ee_messenger.db
```

To reset the database, simply delete this file and restart the application.

## Security Considerations

| Aspect | Implementation |
|--------|----------------|
| Message Encryption | AES-256-GCM (authenticated encryption) |
| Key Exchange | RSA-2048-OAEP with SHA-256 |
| Key Storage | Private key in browser localStorage only |
| Transport | HTTPS recommended for production |
| Authentication | JWT with BCrypt password hashing |

### What the server CANNOT do:
- Read message contents
- Decrypt any messages
- Access user private keys
- Perform man-in-the-middle attacks on message content

### Limitations:
- Private keys stored in browser localStorage (cleared on browser data clear)
- No key backup/recovery mechanism
- Single device per user (key regenerated on new device login)

## Configuration

Edit `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:sqlite:e2ee_messenger.db

jwt:
  secret: YourSecretKeyHere  # Change in production!
  expiration: 86400000       # 24 hours
```

## Screenshots

### Login Page
Clean, modern authentication interface with E2EE badge.

### Chat Interface
Real-time encrypted messaging with encryption status indicators.

## License

MIT License

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

**Note:** This is a demonstration project. For production use, implement additional security measures like HTTPS, key backup mechanisms, and proper key management.
