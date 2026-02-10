package com.ahold.technl.sandbox.service

import com.ahold.technl.sandbox.client.InvoiceServiceClient
import com.ahold.technl.sandbox.client.InvoiceServiceResponse
import com.ahold.technl.sandbox.dto.CreateDeliveryRequest
import com.ahold.technl.sandbox.dto.InvoiceRequest
import com.ahold.technl.sandbox.exception.DeliveryNotFoundException
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
import org.mockito.kotlin.*
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class DeliveryServiceTest {

    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @Mock
    private lateinit var invoiceServiceClient: InvoiceServiceClient

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

    @Test
    fun `sendInvoices should successfully send invoices for valid deliveries`() {
        // Given
        val deliveryId1 = UUID.randomUUID()
        val deliveryId2 = UUID.randomUUID()

        val delivery1 = Delivery(
            id = deliveryId1,
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = Instant.parse("2024-02-04T11:00:00Z"),
            status = DeliveryStatus.DELIVERED
        )

        val delivery2 = Delivery(
            id = deliveryId2,
            vehicleId = "AHV-456",
            address = "Test Street 2",
            startedAt = Instant.parse("2024-02-04T12:00:00Z"),
            finishedAt = Instant.parse("2024-02-04T13:00:00Z"),
            status = DeliveryStatus.DELIVERED
        )

        val invoiceId1 = UUID.randomUUID()
        val invoiceId2 = UUID.randomUUID()

        val request = InvoiceRequest(deliveryIds = listOf(deliveryId1, deliveryId2))

        whenever(deliveryRepository.findAllById(request.deliveryIds!!))
            .thenReturn(listOf(delivery1, delivery2))

        whenever(invoiceServiceClient.sendInvoice(deliveryId1, "Test Street 1"))
            .thenReturn(InvoiceServiceResponse(id = invoiceId1, sent = true))

        whenever(invoiceServiceClient.sendInvoice(deliveryId2, "Test Street 2"))
            .thenReturn(InvoiceServiceResponse(id = invoiceId2, sent = true))

        // When
        val response = deliveryService.sendInvoices(request)

        // Then
        assertEquals(2, response.size)
        assertEquals(deliveryId1, response[0].deliveryId)
        assertEquals(invoiceId1, response[0].invoiceId)
        assertEquals(deliveryId2, response[1].deliveryId)
        assertEquals(invoiceId2, response[1].invoiceId)

        verify(deliveryRepository, times(1)).findAllById(request.deliveryIds!!)
        verify(invoiceServiceClient, times(1)).sendInvoice(deliveryId1, "Test Street 1")
        verify(invoiceServiceClient, times(1)).sendInvoice(deliveryId2, "Test Street 2")
    }

    @Test
    fun `sendInvoices should throw exception when delivery not found`() {
        // Given
        val existingId = UUID.randomUUID()
        val missingId = UUID.randomUUID()

        val delivery = Delivery(
            id = existingId,
            vehicleId = "AHV-123",
            address = "Test Street 1",
            startedAt = Instant.parse("2024-02-04T10:00:00Z"),
            finishedAt = Instant.parse("2024-02-04T11:00:00Z"),
            status = DeliveryStatus.DELIVERED
        )

        val request = InvoiceRequest(deliveryIds = listOf(existingId, missingId))

        // findAllById only returns existing delivery, missing one is not found
        whenever(deliveryRepository.findAllById(request.deliveryIds!!))
            .thenReturn(listOf(delivery))

        // When & Then
        val exception = assertThrows<DeliveryNotFoundException> {
            deliveryService.sendInvoices(request)
        }

        assertTrue(exception.message!!.contains(missingId.toString()))
        verify(deliveryRepository, times(1)).findAllById(request.deliveryIds!!)
        verify(invoiceServiceClient, never()).sendInvoice(any(), any())
    }

    @Test
    fun `sendInvoices should throw exception for empty delivery list`() {
        // Given
        val request = InvoiceRequest(deliveryIds = emptyList())

        // When & Then
        // This will be caught by @NotEmpty validation in the controller
        // But we can still test service behavior
        whenever(deliveryRepository.findAllById(emptyList())).thenReturn(emptyList())

        val response = deliveryService.sendInvoices(request)

        assertEquals(0, response.size)
    }
}