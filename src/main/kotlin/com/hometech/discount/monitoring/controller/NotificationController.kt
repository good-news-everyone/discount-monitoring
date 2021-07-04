package com.hometech.discount.monitoring.controller

import com.hometech.discount.monitoring.domain.model.NotificationRequest
import com.hometech.discount.monitoring.service.NotifyService
import kotlinx.coroutines.ObsoleteCoroutinesApi
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ObsoleteCoroutinesApi
@RestController
class NotificationController(private val notifyService: NotifyService) {

    @PostMapping("/notify/{userId}")
    fun notifyUser(@PathVariable userId: Int, @RequestBody notificationRequest: NotificationRequest) {
        notifyService.sendMessageToUser(userId, notificationRequest.message)
    }

    @PostMapping("/notify")
    fun notifyAllUsers(@RequestBody notificationRequest: NotificationRequest) {
        notifyService.sendMessageToAllUsers(notificationRequest.message)
    }
}
