package com.motaamneh.mstsocialmvp.shared.config;

import com.motaamneh.mstsocialmvp.provider.application.BioProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ProviderDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(ProviderDiagnostics.class);

    @Bean
    ApplicationRunner logBioProvider(BioProvider provider,
                                     @Value("${mst.instagram-provider.mode}") String mode,
                                     @Value("${mst.searchapi.api-key:}") String searchApiKey) {
        return args -> {
            log.info("Instagram biography provider active: mode={}, adapter={}",
                    mode, provider.getClass().getSimpleName());
            if (!searchApiKey.isBlank() && !"searchapi".equals(mode)) {
                log.warn("SearchAPI key is configured but inactive. Set MST_INSTAGRAM_PROVIDER_MODE=searchapi "
                        + "in the application run configuration to read real Instagram biographies.");
            }
        };
    }
}
