package com.unibond.push.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;

@Service
public class LiveActivityPushService {
    private static final Logger log = LoggerFactory.getLogger(LiveActivityPushService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ApnsClient apnsClient;

    @Value("${app.apns.topic:com.unibond.app}")
    private String topic;

    public void updateMoodActivity(String pushToken, String emoji, String text) {
        if (apnsClient == null || pushToken == null) return;

        try {
            Map<String, Object> payload = Map.of(
                "aps", Map.of(
                    "timestamp", Instant.now().getEpochSecond(),
                    "event", "update",
                    "content-state", Map.of(
                        "emoji", emoji != null ? emoji : "",
                        "text", text != null ? text : "",
                        "updatedAt", Instant.now().toString()
                    )
                )
            );
            String payloadJson = objectMapper.writeValueAsString(payload);

            var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(pushToken),
                topic + ".push-type.liveactivity",
                payloadJson);
            apnsClient.sendNotification(notification);
        } catch (Exception e) {
            log.error("Failed to update Live Activity", e);
        }
    }
}
