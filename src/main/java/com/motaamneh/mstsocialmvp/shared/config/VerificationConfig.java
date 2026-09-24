package com.motaamneh.mstsocialmvp.shared.config;

import com.motaamneh.mstsocialmvp.verification.domain.MarkerService;
import java.security.SecureRandom;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VerificationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    MarkerService markerService(@Value("${mst.security.marker-hmac-key}") String secret) {
        return new MarkerService(new SecureRandom(), secret);
    }
}
