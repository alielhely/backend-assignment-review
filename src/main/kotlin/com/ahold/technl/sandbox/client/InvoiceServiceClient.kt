package com.ahold.technl.sandbox.client

import com.ahold.technl.sandbox.exception.InvoiceServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.UUID

@Component
class InvoiceServiceClient(
    @Value("\${invoice-service.url}") private val invoiceServiceUrl: String,
    private val objectMapper: ObjectMapper
) {
    private val restClient = RestClient.create()
    private val logger = LoggerFactory.getLogger(InvoiceServiceClient::class.java)

    @CircuitBreaker(name = "invoiceService", fallbackMethod = "sendInvoiceFallback")
    @Retry(name = "invoiceService")
    fun sendInvoice(deliveryId: UUID, address: String): InvoiceServiceResponse {
        logger.info("Attempting to send invoice for delivery: $deliveryId")

        try {
            val request = InvoiceServiceRequest(deliveryId, address)
            val jsonBody = objectMapper.writeValueAsString(request)

            logger.info("Sending request body: $jsonBody")

            val response = restClient.post()
                .uri("$invoiceServiceUrl/v1/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .retrieve()
                .body(InvoiceServiceResponse::class.java)

            if (response == null) {
                logger.error("Empty response from invoice service for delivery: $deliveryId")
                throw InvoiceServiceException("Empty response from invoice service")
            }

            logger.info("Successfully sent invoice for delivery: $deliveryId, invoiceId: ${response.id}")
            return response

        } catch (e: RestClientException) {
            logger.error("Failed to send invoice for delivery: $deliveryId", e)
            throw InvoiceServiceException("Failed to send invoice: ${e.message}")
        }
    }

    private fun sendInvoiceFallback(deliveryId: UUID, address: String, ex: Exception): InvoiceServiceResponse {
        logger.warn("Circuit breaker OPEN or retries exhausted for delivery: $deliveryId. Using fallback. Error: ${ex.message}")

        return InvoiceServiceResponse(
            id = UUID.randomUUID(),
            sent = false
        )
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