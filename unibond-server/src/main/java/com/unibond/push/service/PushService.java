package com.unibond.push.service;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushService {
    private static final Logger log = LoggerFactory.getLogger(PushService.class);

    @Autowired(required = false)
    private ApnsClient apnsClient;

    @Value("${app.apns.topic:com.unibond.app}")
    private String topic;

    public void sendPush(String deviceToken, String title, String body) {
        if (apnsClient == null || deviceToken == null) return;

        try {
            String payload = new SimpleApnsPayloadBuilder()
                .setAlertTitle(title)
                .setAlertBody(body)
                .setSound("default")
                .build();

            var notification = new SimpleApnsPushNotification(
                TokenUtil.sanitizeTokenString(deviceToken), topic, payload);
            apnsClient.sendNotification(notification).whenComplete((resp, cause) -> {
                if (cause != null) {
                    log.error("APNs send failed", cause);
                } else if (!resp.isAccepted()) {
                    log.warn("APNs rejected: {}", resp.getRejectionReason());
                }
            });
        } catch (Exception e) {
            log.error("Failed to send push", e);
        }
    }
}
