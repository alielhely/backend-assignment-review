package com.ahold.technl.sandbox.service

import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.dto.DeliveryResponse
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
import com.ahold.technl.sandbox.model.Delivery
import com.ahold.technl.sandbox.model.DeliveryStatus
import com.ahold.technl.sandbox.repository.DeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository
) {

    @Transactional
    fun createDelivery(request: CreateDeliveryRequest): DeliveryResponse {
        validateDeliveryRequest(request)

        val delivery = Delivery(
            vehicleId = request.vehicleId!!,
            address = request.address!!,
            startedAt = request.startedAt!!,
            finishedAt = request.finishedAt,
            status = request.status!!
        )

        val saved = deliveryRepository.save(delivery)
        return saved.toResponse()
    }

    private fun validateDeliveryRequest(request: CreateDeliveryRequest) {
        when (request.status ?: throw InvalidDeliveryStateException("Status must not be null nor blank")) {
            DeliveryStatus.IN_PROGRESS -> {
                if (request.finishedAt != null) {
                    throw InvalidDeliveryStateException(
                        "IN_PROGRESS deliveries must not have finishedAt"
                    )
                }
            }
            DeliveryStatus.DELIVERED -> {
                if (request.finishedAt == null) {
                    throw InvalidDeliveryStateException(
                        "DELIVERED deliveries must have finishedAt"
                    )
                }
            }
        }
    }

    private fun Delivery.toResponse() = DeliveryResponse(
        id = id!!,
        vehicleId = vehicleId,
        address = address,
        startedAt = startedAt,
        finishedAt = finishedAt,
        status = status
    )
}