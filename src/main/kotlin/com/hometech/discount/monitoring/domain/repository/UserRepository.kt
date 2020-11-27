package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.BotUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepository : JpaRepository<BotUser, Int> {

    @Query("select u from ItemSubscriber i join BotUser u on i.userId = u.id where i.itemId = :itemId")
    fun findAllUsersSubscribedOnItem(@Param("itemId") itemId: Long): BotUser
}
