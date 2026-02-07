package com.ahold.technl.sandbox.dto

import com.ahold.technl.sandbox.model.DeliveryStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateDeliveryRequest(
    @field:NotBlank(message = "Vehicle ID must not be blank")
    val vehicleId: String,

    @field:NotBlank(message = "Address must not be blank")
    val address: String,

    @field:NotNull(message = "Status must not be blank")
    val startedAt: Instant,

    val finishedAt: Instant? = null,

    @field:NotNull(message = "Status must not be blank")
    val status: DeliveryStatus

    )

data class DeliveryResponse(
    val id: UUID,
    val vehicleId: String,
    val address: String,
    val startedAt: Instant,
    val finishedAt: Instant?,
    val status: DeliveryStatus
)

data class InvoiceRequest(
    @field:NotEmpty(message = "Delivery IDs must not be empty")
    @field:Size(min = 1, message = "At least one delivery ID must be provided")
    val deliveryIds: List<UUID>
)

data class InvoiceResponse(
    val deliveryId: UUID,
    val invoiceId: UUID
)

data class BusinessSummaryResponse(
    val deliveries: Long,
    val averageMinutesBetweenDeliveryStart: Long
)
