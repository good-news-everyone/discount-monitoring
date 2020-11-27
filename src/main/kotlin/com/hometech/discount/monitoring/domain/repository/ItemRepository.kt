package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.Item
import org.springframework.data.jpa.repository.JpaRepository

interface ItemRepository : JpaRepository<Item, Long> {
    fun existsByUrl(url: String): Boolean
    fun findOneByUrl(url: String): Item
}
