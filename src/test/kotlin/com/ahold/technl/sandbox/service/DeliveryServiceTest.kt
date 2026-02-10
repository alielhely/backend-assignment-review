package com.ahold.technl.sandbox.service

import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
import com.ahold.technl.sandbox.model.Delivery
import com.ahold.technl.sandbox.model.DeliveryStatus
import com.ahold.technl.sandbox.repository.DeliveryRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(MockitoExtension::class)
class DeliveryServiceTest {

    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @InjectMocks
    private lateinit var deliveryService: DeliveryService

    @Test
    fun `createDelivery should save valid IN_PROGRESS delivery`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.IN_PROGRESS
        )

        val savedDelivery = Delivery(
            id = UUID.randomUUID(),
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.IN_PROGRESS
        )

        whenever(deliveryRepository.save(any())).thenReturn(savedDelivery)

        // When
        val response = deliveryService.createDelivery(request)

        // Then
        assertNotNull(response.id)
        assertEquals("AHV-123", response.vehicleId)
        assertEquals("Test Street 1", response.address)
        assertEquals(DeliveryStatus.IN_PROGRESS, response.status)
        assertEquals(null, response.finishedAt)
        verify(deliveryRepository, times(1)).save(any())
    }

    @Test
    fun `createDelivery should save valid DELIVERED delivery`() {
        // Given
        val finishedTime = Instant.parse("2024-02-04T15:00:00Z")
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-456",
            address = "Test Street 2",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = finishedTime,
            status = DeliveryStatus.DELIVERED
        )

        val savedDelivery = Delivery(
            id = UUID.randomUUID(),
            vehicleId = "AHV-456",
            address = "Test Street 2",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = finishedTime,
            status = DeliveryStatus.DELIVERED
        )

        whenever(deliveryRepository.save(any())).thenReturn(savedDelivery)

        // When
        val response = deliveryService.createDelivery(request)

        // Then
        assertNotNull(response.id)
        assertEquals(DeliveryStatus.DELIVERED, response.status)
        assertEquals(finishedTime, response.finishedAt)
        verify(deliveryRepository, times(1)).save(any())
    }

    @Test
    fun `createDelivery should throw exception when IN_PROGRESS has finishedAt`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-789",
            address = "Test Street 3",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = Instant.parse("2024-02-04T11:00:00Z"),
            status = DeliveryStatus.IN_PROGRESS
        )

        // When & Then
        val exception = assertThrows<InvalidDeliveryStateException> {
            deliveryService.createDelivery(request)
        }

        assertEquals("IN_PROGRESS deliveries must not have finishedAt", exception.message)
        verify(deliveryRepository, never()).save(any())
    }

    @Test
    fun `createDelivery should throw exception when DELIVERED missing finishedAt`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-999",
            address = "Test Street 4",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = DeliveryStatus.DELIVERED
        )

        // When & Then
        val exception = assertThrows<InvalidDeliveryStateException> {
            deliveryService.createDelivery(request)
        }

        assertEquals("DELIVERED deliveries must have finishedAt", exception.message)
        verify(deliveryRepository, never()).save(any())
    }

    @Test
    fun `createDelivery should throw exception when status is null`() {
        // Given
        val request = CreateDeliveryRequest(
            vehicleId = "AHV-111",
            address = "Test Street 5",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = null,
            status = null
        )

        // When & Then
        val exception = assertThrows<InvalidDeliveryStateException> {
            deliveryService.createDelivery(request)
        }

        assertEquals("Status must not be null nor blank", exception.message)
        verify(deliveryRepository, never()).save(any())
    }
}