package com.unibond.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AppleAuthService {
    private static final Logger log = LoggerFactory.getLogger(AppleAuthService.class);
    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String clientId;
    private final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();
    private long keysFetchedAt = 0;
    private static final long KEY_CACHE_TTL_MS = 24 * 60 * 60 * 1000;

    public AppleAuthService(@Value("${apple.client-id:com.unibond.app}") String clientId) {
        this.clientId = clientId;
    }

    public String extractAppleSub(String identityToken) {
        try {
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid JWT format");

            // Decode header to get kid
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode header = objectMapper.readTree(headerJson);
            String kid = header.get("kid").asText();

            RSAPublicKey publicKey = getApplePublicKey(kid);

            Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(identityToken)
                .getPayload();

            // Validate issuer
            if (!APPLE_ISSUER.equals(claims.getIssuer())) {
                throw new IllegalArgumentException("Invalid Apple token issuer");
            }
            // Validate audience
            if (!claims.getAudience().contains(clientId)) {
                throw new IllegalArgumentException("Invalid Apple token audience");
            }

            String sub = claims.getSubject();
            if (sub == null || sub.isBlank()) throw new IllegalArgumentException("Missing sub claim");
            return sub;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Apple identity token", e);
        }
    }

    private RSAPublicKey getApplePublicKey(String kid) throws Exception {
        fetchKeysIfNeeded();
        RSAPublicKey key = keyCache.get(kid);
        if (key == null) {
            // Force refresh and retry
            keysFetchedAt = 0;
            fetchKeysIfNeeded();
            key = keyCache.get(kid);
        }
        if (key == null) throw new IllegalArgumentException("Unknown Apple signing key: " + kid);
        return key;
    }

    private synchronized void fetchKeysIfNeeded() throws Exception {
        if (!keyCache.isEmpty() && System.currentTimeMillis() - keysFetchedAt < KEY_CACHE_TTL_MS) {
            return;
        }
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(APPLE_KEYS_URL))
            .GET().build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode keysNode = objectMapper.readTree(response.body()).get("keys");
        for (JsonNode jwk : keysNode) {
            String n = jwk.get("n").asText();
            String e = jwk.get("e").asText();
            String k = jwk.get("kid").asText();

            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);

            var modulus = new java.math.BigInteger(1, nBytes);
            var exponent = new java.math.BigInteger(1, eBytes);
            var spec = new RSAPublicKeySpec(modulus, exponent);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
            keyCache.put(k, pubKey);
        }
        keysFetchedAt = System.currentTimeMillis();
    }
}
