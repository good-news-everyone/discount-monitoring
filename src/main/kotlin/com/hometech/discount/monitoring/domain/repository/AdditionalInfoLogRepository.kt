package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.AdditionalInfoLog
import org.springframework.data.jpa.repository.JpaRepository

interface AdditionalInfoLogRepository : JpaRepository<AdditionalInfoLog, Long>
