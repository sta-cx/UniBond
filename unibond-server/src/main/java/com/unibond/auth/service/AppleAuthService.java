package com.unibond.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class AppleAuthService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract Apple 'sub' from identity token JWT.
     * MVP: decode payload without full signature verification.
     * Production: validate against Apple's public keys (https://appleid.apple.com/auth/keys).
     */
    public String extractAppleSub(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode node = objectMapper.readTree(payload);
            String sub = node.get("sub").asText();
            if (sub == null || sub.isBlank()) throw new IllegalArgumentException("Missing sub claim");
            return sub;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Apple identity token", e);
        }
    }
}
