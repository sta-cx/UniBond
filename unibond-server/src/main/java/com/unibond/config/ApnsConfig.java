package com.unibond.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.File;

@Configuration
@ConditionalOnProperty(name = "app.apns.key-path")
public class ApnsConfig {
    @Bean
    public ApnsClient apnsClient(
            @Value("${app.apns.key-path:}") String keyPath,
            @Value("${app.apns.key-id:}") String keyId,
            @Value("${app.apns.team-id:}") String teamId,
            @Value("${app.apns.production:false}") boolean production) {
        if (keyPath == null || keyPath.isBlank() || keyId == null || keyId.isBlank()) {
            return null;
        }
        
        File keyFile = new File(keyPath);
        if (!keyFile.exists()) {
            return null;
        }

        try {
            return new ApnsClientBuilder()
                .setApnsServer(production
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(com.eatthepath.pushy.apns.auth.ApnsSigningKey.loadFromPkcs8File(
                    keyFile, teamId, keyId))
                .build();
        } catch (Exception e) {
            return null;
        }
    }
}
