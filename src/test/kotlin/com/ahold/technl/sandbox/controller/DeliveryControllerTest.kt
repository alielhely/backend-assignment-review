package com.ahold.technl.sandbox.controller

import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.dto.DeliveryResponse
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID

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
}