package com.ahold.technl.sandbox.repository

import com.ahold.technl.sandbox.model.Delivery
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface DeliveryRepository : JpaRepository<Delivery, UUID> {

    @Query("SELECT d FROM Delivery d WHERE d.startedAt >= :start AND d.startedAt < :end ORDER BY d.startedAt ASC")
    fun findByStartedAtBetweenOrderByStartedAtAsc(start: Instant, end: Instant): List<Delivery>
}
