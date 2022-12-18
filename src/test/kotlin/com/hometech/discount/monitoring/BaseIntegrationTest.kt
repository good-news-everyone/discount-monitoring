package com.hometech.discount.monitoring

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.AfterEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [MonitoringApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class BaseIntegrationTest {

    @AfterEach
    fun resetWireMock() {
        WireMock.reset()
    }
}
