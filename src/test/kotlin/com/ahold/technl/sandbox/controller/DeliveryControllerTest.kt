package com.ahold.technl.sandbox.controller

import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.dto.DeliveryResponse
import com.ahold.technl.sandbox.dto.InvoiceRequest
import com.ahold.technl.sandbox.dto.InvoiceResponse
import com.ahold.technl.sandbox.exception.DeliveryNotFoundException
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
import com.ahold.technl.sandbox.exception.InvoiceServiceException
import com.ahold.technl.sandbox.model.DeliveryStatus
import com.ahold.technl.sandbox.service.DeliveryService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.*

@WebMvcTest(DeliveryController::class)
class DeliveryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var deliveryService: DeliveryService

    @Test
    fun `POST deliveries should return 201 for valid IN_PROGRESS delivery`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.IN_PROGRESS
        )

        val response = DeliveryResponse(
            id = UUID.randomUUID(),
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.IN_PROGRESS
        )

        whenever(deliveryService.createDelivery(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.vehicleId").value("AHV-123"))
            .andExpect(jsonPath("$.address").value("Test Street 1"))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.finishedAt").isEmpty)
    }

    @Test
    fun `POST deliveries should return 201 for valid DELIVERED delivery`() {
        // Given
        val finishedTime = Instant.parse("2024-02-04T15:00:00Z")
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-456",
            address = "Test Street 2",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = finishedTime,
            status = DeliveryStatus.DELIVERED
        )

        val response = DeliveryResponse(
            id = UUID.randomUUID(),
            vehicleId = "AHV-456",
            address = "Test Street 2",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = finishedTime,
            status = DeliveryStatus.DELIVERED
        )

        whenever(deliveryService.createDelivery(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("DELIVERED"))
            .andExpect(jsonPath("$.finishedAt").value("2024-02-04T15:00:00Z"))
    }

    @Test
    fun `POST deliveries should return 400 when address is blank`() {
        // Given
        val request = mapOf(
            "vehicleId" to "AHV-123",
            "address" to "",
            "startedAt" to "2024-02-04T10:00:00Z",
            "status" to "IN_PROGRESS"
        )

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST deliveries should return 400 when address is missing`() {
        // Given
        val request = mapOf(
            "vehicleId" to "AHV-123",
            "startedAt" to "2024-02-04T10:00:00Z",
            "status" to "IN_PROGRESS"
        )

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("address")))
    }

    @Test
    fun `POST deliveries should return 400 when IN_PROGRESS has finishedAt`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-789",
            address = "Test Street 3",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = Instant.parse("2024-02-04T11:00:00Z"),
            status = DeliveryStatus.IN_PROGRESS
        )

        whenever(deliveryService.createDelivery(any()))
            .thenThrow(InvalidDeliveryStateException("IN_PROGRESS deliveries must not have finishedAt"))

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("IN_PROGRESS deliveries must not have finishedAt"))
            .andExpect(jsonPath("$.traceId").exists())
    }

    @Test
    fun `POST deliveries should return 400 when DELIVERED missing finishedAt`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-999",
            address = "Test Street 4",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.DELIVERED
        )

        whenever(deliveryService.createDelivery(any()))
            .thenThrow(InvalidDeliveryStateException("DELIVERED deliveries must have finishedAt"))

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("DELIVERED deliveries must have finishedAt"))
            .andExpect(jsonPath("$.traceId").exists())
    }

    @Test
    fun `POST deliveries should return 400 when vehicleId is blank`() {
        // Given
        val request = mapOf(
            "vehicleId" to "",
            "address" to "Test Street",
            "startedAt" to "2024-02-04T10:00:00Z",
            "status" to "IN_PROGRESS"
        )

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("vehicleId")))
    }

    @Test
    fun `POST deliveries should return 400 when status is missing`() {
        // Given
        val request = mapOf(
            "vehicleId" to "AHV-123",
            "address" to "Test Street",
            "startedAt" to "2024-02-04T10:00:00Z"
        )

        // When & Then
        mockMvc.perform(
            post("/deliveries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Status")))
    }

    @Test
    fun `POST deliveries invoice should return 200 for single delivery`() {
        // Given
        val deliveryId = UUID.randomUUID()
        val invoiceId = UUID.randomUUID()
        val request = InvoiceRequest(deliveryIds = listOf(deliveryId))

        val response = listOf(
            InvoiceResponse(
                deliveryId = deliveryId,
                invoiceId = invoiceId
            )
        )

        whenever(deliveryService.sendInvoices(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].deliveryId").value(deliveryId.toString()))
            .andExpect(jsonPath("$[0].invoiceId").value(invoiceId.toString()))
    }

    @Test
    fun `POST deliveries invoice should return 200 for multiple deliveries`() {
        // Given
        val deliveryId1 = UUID.randomUUID()
        val deliveryId2 = UUID.randomUUID()
        val invoiceId1 = UUID.randomUUID()
        val invoiceId2 = UUID.randomUUID()

        val request = InvoiceRequest(deliveryIds = listOf(deliveryId1, deliveryId2))

        val response = listOf(
            InvoiceResponse(deliveryId = deliveryId1, invoiceId = invoiceId1),
            InvoiceResponse(deliveryId = deliveryId2, invoiceId = invoiceId2)
        )

        whenever(deliveryService.sendInvoices(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].deliveryId").value(deliveryId1.toString()))
            .andExpect(jsonPath("$[0].invoiceId").value(invoiceId1.toString()))
            .andExpect(jsonPath("$[1].deliveryId").value(deliveryId2.toString()))
            .andExpect(jsonPath("$[1].invoiceId").value(invoiceId2.toString()))
    }

    @Test
    fun `POST deliveries invoice should return 400 when deliveryIds is empty`() {
        // Given - empty list violates @Size(min = 1)
        val request = mapOf("deliveryIds" to emptyList<UUID>())

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST deliveries invoice should return 400 when deliveryIds is missing`() {
        // Given
        val request = mapOf<String, Any>()

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `POST deliveries invoice should return 404 when delivery not found`() {
        // Given
        val deliveryId = UUID.randomUUID()
        val request = InvoiceRequest(deliveryIds = listOf(deliveryId))

        whenever(deliveryService.sendInvoices(any()))
            .thenThrow(DeliveryNotFoundException("Delivery not found: $deliveryId"))

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Delivery not found: $deliveryId"))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST deliveries invoice should return 404 when multiple deliveries not found`() {
        // Given
        val deliveryId1 = UUID.randomUUID()
        val deliveryId2 = UUID.randomUUID()
        val request = InvoiceRequest(deliveryIds = listOf(deliveryId1, deliveryId2))

        whenever(deliveryService.sendInvoices(any()))
            .thenThrow(DeliveryNotFoundException("Deliveries not found: $deliveryId1, $deliveryId2"))

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Deliveries not found")))
    }

    @Test
    fun `POST deliveries invoice should return 503 when invoice service unavailable`() {
        // Given
        val deliveryId = UUID.randomUUID()
        val request = InvoiceRequest(deliveryIds = listOf(deliveryId))

        whenever(deliveryService.sendInvoices(any()))
            .thenThrow(InvoiceServiceException("Invoice service is currently unavailable"))

        // When & Then
        mockMvc.perform(
            post("/deliveries/invoice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.error").value("Invoice service is currently unavailable. Please try again later."))
            .andExpect(jsonPath("$.traceId").exists())
            .andExpect(jsonPath("$.timestamp").exists())
    }
}