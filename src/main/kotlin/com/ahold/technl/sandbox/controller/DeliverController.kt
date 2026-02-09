package com.ahold.technl.sandbox.controller

import com.ahold.technl.sandbox.dto.*
import com.ahold.technl.sandbox.service.DeliveryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/deliveries")
class DeliveryController(
    private val deliveryService: DeliveryService
) {

    @PostMapping
    fun createDelivery(
        @Valid @RequestBody request: CreateDeliveryRequest
    ): ResponseEntity<DeliveryResponse> {
        val response = deliveryService.createDelivery(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
