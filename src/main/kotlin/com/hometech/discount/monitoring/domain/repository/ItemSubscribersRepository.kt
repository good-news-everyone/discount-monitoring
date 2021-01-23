package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.ItemSubscriber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ItemSubscribersRepository : JpaRepository<ItemSubscriber, Long> {

    fun existsByItemIdAndUserId(itemId: Long, userId: Long): Boolean
    fun deleteAllByItemIdIn(ids: List<Long>)

    @Query("select count(i_s) from ItemSubscriber i_s where i_s.itemId = (select i.id from Item i where i.url = :url) and i_s.userId = :userId")
    fun countSubscriptionsByUrlAndUserId(@Param("url") url: String, @Param("userId") userId: Long): Int

    @Query(value = "delete from ItemSubscriber i_s where i_s.itemId = (select i.id from Item i where i.url = :url) and i_s.userId = :userId")
    @Modifying
    fun removeSubscriptionByUrlAndUserId(@Param("url") url: String, @Param("userId") userId: Long)

    @Query("delete from ItemSubscriber where userId = :userId")
    @Modifying
    fun clearUserSubscriptions(@Param("userId") userId: Long)
}
