package com.innerderma.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataInitializerTest {

    @Test
    void isExcludedFromProdProfile() {
        Profile profile = DemoDataInitializer.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("!prod");
    }
}
