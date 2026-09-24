package com.motaamneh.mstsocialmvp.verification.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class InstagramHandleTest {
    @Test
    void normalizesOptionalAtAndCase() {
        assertThat(InstagramHandle.parse("  @Example_User  ").value()).isEqualTo("example_user");
    }

    @Test
    void rejectsUrlsAndMalformedHandles() {
        for (String input : new String[]{"https://instagram.com/example", "a b", "a/b", "a?b", "a#b",
                "@", "a..b", ".ab", "ab.", "a".repeat(31)}) {
            assertThatThrownBy(() -> InstagramHandle.parse(input))
                    .as("input: %s", input)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
