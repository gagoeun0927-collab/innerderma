package com.innerderma.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataInitializerTest {

    @Test
    void runsOnAllProfilesIncludingProd() {
        // 시연/대회용으로 모든 환경에서 데모 데이터 시드가 실행된다.
        Profile profile = DemoDataInitializer.class.getAnnotation(Profile.class);
        assertThat(profile).isNull();
    }
}
