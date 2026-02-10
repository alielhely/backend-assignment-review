package com.ahold.technl.sandbox.service

import com.ahold.technl.sandbox.client.InvoiceServiceClient
import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.dto.DeliveryResponse
import com.ahold.technl.sandbox.dto.InvoiceRequest
import com.ahold.technl.sandbox.dto.InvoiceResponse
import com.ahold.technl.sandbox.exception.DeliveryNotFoundException
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
import com.ahold.technl.sandbox.model.Delivery
import com.ahold.technl.sandbox.model.DeliveryStatus
import com.ahold.technl.sandbox.repository.DeliveryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeliveryService(
    private val deliveryRepository: DeliveryRepository,
    private val invoiceServiceClient: InvoiceServiceClient
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

    @Transactional
    fun sendInvoices(request: InvoiceRequest): List<InvoiceResponse> {
        val deliveryIds = request.deliveryIds
            ?: throw InvalidDeliveryStateException("Delivery IDs must not be null")
        val deliveries = deliveryRepository.findAllById(request.deliveryIds)
        val foundIds = deliveries.map { it.id }.toSet()

        val missingIds = request.deliveryIds.filterNot { it in foundIds }
        if (missingIds.isNotEmpty()) {
            throw DeliveryNotFoundException("Deliveries not found: ${missingIds.joinToString(", ")}")
        }

        val results = mutableListOf<InvoiceResponse>()
        for (delivery in deliveries) {
            val invoiceResponse = invoiceServiceClient.sendInvoice(delivery.id!!, delivery.address)

            results.add(InvoiceResponse(
                deliveryId = delivery.id!!,
                invoiceId = invoiceResponse.id
            ))
        }

        return results
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