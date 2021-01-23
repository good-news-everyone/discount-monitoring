package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.Item
import com.hometech.discount.monitoring.parser.ParserType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ItemRepository : JpaRepository<Item, Long> {
    fun existsByUrl(url: String): Boolean
    fun findOneByUrl(url: String): Item
    fun findAllByType(type: ParserType): List<Item>

    @Query("select i from Item i join ItemSubscriber i_s on i_s.itemId = i.id where i_s.userId = :userId")
    fun findItemsByUserId(@Param("userId") userId: Long): List<Item>

    @Query("delete from Item where id in (select i from Item i left join ItemSubscriber i_s on i_s.itemId = i.id where i_s.id is null)")
    @Modifying
    fun deleteItemsWithoutSubscriptions()
}
