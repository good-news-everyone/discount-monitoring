package com.hometech.discount.monitoring

import com.hometech.discount.monitoring.domain.repository.ItemRepository
import com.hometech.discount.monitoring.domain.repository.ItemSubscribersRepository
import com.hometech.discount.monitoring.domain.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(
    classes = [MonitoringApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class BaseIntegrationTest {
    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var subscribersRepository: ItemSubscribersRepository
}
