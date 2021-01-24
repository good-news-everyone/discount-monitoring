package com.hometech.discount.monitoring.domain.entity

import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.parser.ParserType
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "items")
@TypeDefs(
    value = [
        TypeDef(name = "json", typeClass = JsonStringType::class),
        TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
    ]
)
class Item(
    val url: String,
    val name: String,
    val priceCurrency: String,
    price: BigDecimal
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    val type: ParserType = ParserType.findByUrl(url)

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    var additionalInfo = AdditionalInfo()

    var initialPrice: BigDecimal = price
    var lowestPrice: BigDecimal = price
    var currentPrice: BigDecimal = price
    var highestPrice: BigDecimal = price
    var timeAdded: LocalDateTime = LocalDateTime.now()

    fun setNewPrice(newPrice: BigDecimal) {
        this.currentPrice = newPrice
        if (currentPrice < lowestPrice)
            this.lowestPrice = newPrice
        if (currentPrice > highestPrice)
            this.highestPrice = newPrice
    }

    override fun toString(): String {
        return "Item(id = $id, type = $type, url = $url)"
    }
}
