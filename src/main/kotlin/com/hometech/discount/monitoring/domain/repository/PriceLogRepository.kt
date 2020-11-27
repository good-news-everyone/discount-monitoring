package com.hometech.discount.monitoring.domain.repository

import com.hometech.discount.monitoring.domain.entity.PriceLog
import org.springframework.data.jpa.repository.JpaRepository

interface PriceLogRepository : JpaRepository<PriceLog, Long>
