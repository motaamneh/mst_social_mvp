package com.motaamneh.mstsocialmvp.tenant.infrastructure;

import com.motaamneh.mstsocialmvp.tenant.application.TenantApiKeys;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalTenantBootstrap {
    private static final UUID LOCAL_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Bean
    ApplicationRunner localTenantRunner(TenantJpaRepository tenants, TenantApiKeyJpaRepository keys,
                                        Clock clock, @Value("${MST_LOCAL_API_KEY:}") String key) {
        return args -> {
            if (key.isBlank()) {
                return;
            }
            if (!key.startsWith("mst_test_") || !TenantApiKeys.isValid(key)) {
                throw new IllegalArgumentException("MST_LOCAL_API_KEY must be mst_test_ followed by 64 hex characters");
            }
            if (!tenants.existsById(LOCAL_TENANT_ID)) {
                tenants.save(new TenantEntity(LOCAL_TENANT_ID, "Local development", "ACTIVE", clock.instant()));
            }
            byte[] digest = TenantApiKeys.digest(key);
            boolean exists = keys.findByKeyPrefixAndStatus(TenantApiKeys.prefix(key), "ACTIVE").stream()
                    .anyMatch(candidate -> MessageDigest.isEqual(candidate.getKeyDigest(), digest));
            if (!exists) {
                keys.save(new TenantApiKeyEntity(UUID.randomUUID(), LOCAL_TENANT_ID,
                        TenantApiKeys.prefix(key), digest, "ACTIVE", clock.instant()));
            }
        };
    }
}
