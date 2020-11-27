package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import org.springframework.data.jpa.repository.JpaRepository

interface ItemSubscribersRepository : JpaRepository<ItemSubscriber, Long>
