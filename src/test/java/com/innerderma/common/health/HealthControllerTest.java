package com.innerderma.common.health;

import com.innerderma.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void returnsServerStatusUsingCommonResponse() {
        HealthController controller = new HealthController();

        ApiResponse<Map<String, String>> response = controller.health();

        assertThat(response.success()).isTrue();
        assertThat(response.data().get("status")).isEqualTo("ok");
    }
}
