package com.motaamneh.mstsocialmvp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "mst.security.marker-hmac-key=test-only-marker-hmac-key-at-least-32-bytes")
class MstSocialMvpApplicationTests {

    @Test
    void contextLoads() {
    }

}
