package com.motaamneh.mstsocialmvp.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

class ProviderWiringTest {
    @Test
    void searchApiModeStartsItsProvider() {
        new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(SearchApiBioProvider.class)
                .withPropertyValues("mst.instagram-provider.mode=searchapi", "mst.searchapi.api-key=test-key")
                .run(context -> assertThat(context).hasSingleBean(SearchApiBioProvider.class));
    }

    @Test
    void httpModeStartsItsProvider() {
        new ApplicationContextRunner()
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withUserConfiguration(HttpBioProvider.class)
                .withPropertyValues("mst.instagram-provider.mode=http",
                        "mst.instagram-provider.base-url=https://provider.example/v1",
                        "mst.instagram-provider.allowed-host=provider.example",
                        "mst.instagram-provider.token=test-token")
                .run(context -> assertThat(context).hasSingleBean(HttpBioProvider.class));
    }
}
