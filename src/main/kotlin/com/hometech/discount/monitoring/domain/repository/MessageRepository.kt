package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.Message
import org.springframework.data.jpa.repository.JpaRepository

interface MessageRepository : JpaRepository<Message, Int>
