package com.hometech.discount.monitoring.domain.entity

import com.hometech.discount.monitoring.domain.model.AdditionalInfo
import com.hometech.discount.monitoring.domain.model.SizeInfo
import com.vladmihalcea.hibernate.type.json.JsonBinaryType
import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "additional_info_changelog")
@TypeDefs(
    value = [
        TypeDef(name = "json", typeClass = JsonStringType::class),
        TypeDef(name = "jsonb", typeClass = JsonBinaryType::class)
    ]
)
class AdditionalInfoLog(
    val itemId: Long,
    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    val infoBefore: AdditionalInfo,

    @Type(type = "jsonb")
    @Column(columnDefinition = "json")
    val infoNow: AdditionalInfo
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
    val timeChecked = LocalDateTime.now()

    fun difference(): String {
        return if (infoBefore.sizes != null || infoNow.sizes != null) {
            val sizesBefore = infoBefore.sizes?.associateBy { it.name } ?: mapOf()
            val sizesNow = infoNow.sizes?.associateBy { it.name } ?: mapOf()
            sizesNow.entries.mapNotNull {
                if (it.value.availability != sizesBefore[it.key]?.availability) {
                    it.value.availabilityMessage()
                } else null
            }.joinToString(separator = "\n")
        } else ""
    }

    private fun SizeInfo.availabilityMessage(): String {
        return if (this.availability)
            "Размер ${this.name} появился в наличии!"
        else "Размера ${this.name} больше нет в наличии!"
    }
}
