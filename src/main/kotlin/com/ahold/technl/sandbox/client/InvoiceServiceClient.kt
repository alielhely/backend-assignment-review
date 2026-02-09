package com.ahold.technl.sandbox.client

import com.ahold.technl.sandbox.exception.InvoiceServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

@Component
class InvoiceServiceClient(
    @Value("\${invoice-service.url}") private val invoiceServiceUrl: String
) {
    private val restClient = RestClient.create()

    fun sendInvoice(deliveryId: UUID, address: String): InvoiceServiceResponse {
        try {
            val request = InvoiceServiceRequest(deliveryId, address)

            val response = restClient.post()
                .uri("$invoiceServiceUrl/v1/invoices")
                .body(request)
                .retrieve()
                .body(InvoiceServiceResponse::class.java)

            return response ?: throw InvoiceServiceException("Empty response from invoice service")
        } catch (e: RestClientException) {
            throw InvoiceServiceException("Failed to send invoice: ${e.message}")
        }
    }
}

data class InvoiceServiceRequest(
    val deliveryId: UUID,
    val address: String
)

data class InvoiceServiceResponse(
    val id: UUID,
    val sent: Boolean
)
