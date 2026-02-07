package com.ahold.technl.sandbox.model

import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "deliveries")
data class Delivery(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,
    @Column(nullable = false)
    val vehicleId: String,
    @Column(nullable = false)
    val address: String,
    @Column(nullable = false)
    val startedAt: Instant,
    @Column
    val finishedAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: DeliveryStatus
)

enum class DeliveryStatus {
    IN_PROGRESS, DELIVERED
}


