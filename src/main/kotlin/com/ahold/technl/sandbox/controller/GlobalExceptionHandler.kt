package com.ahold.technl.sandbox.controller

import com.ahold.technl.sandbox.exception.DeliveryNotFoundException
import com.ahold.technl.sandbox.exception.InvalidDeliveryStateException
import com.ahold.technl.sandbox.exception.InvoiceServiceException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant
import java.util.UUID

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(InvalidDeliveryStateException::class)
    fun handleInvalidDeliveryState(ex: InvalidDeliveryStateException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.warn("Invalid delivery state [traceId: $traceId]: ${ex.message}", ex)

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = ex.message ?: "Invalid delivery state",
                traceId = traceId,
                timestamp = Instant.now()
            ))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString(", ")

        logger.warn("Validation failed [traceId: $traceId]: $errors")

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(
                error = errors,
                traceId = traceId,
                timestamp = Instant.now()
            ))
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Unexpected error [traceId: $traceId]", ex)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(
                error = "An unexpected error occurred. Please contact support with trace ID: $traceId",
                traceId = traceId,
                timestamp = Instant.now()
            ))
    }

    @ExceptionHandler(DeliveryNotFoundException::class)
    fun handleDeliveryNotFound(ex: DeliveryNotFoundException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.warn("Delivery not found [traceId: $traceId]: ${ex.message}", ex)

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(
                error = ex.message ?: "Delivery not found",
                traceId = traceId,
                timestamp = Instant.now()
            ))
    }

    @ExceptionHandler(InvoiceServiceException::class)
    fun handleInvoiceServiceException(ex: InvoiceServiceException): ResponseEntity<ErrorResponse> {
        val traceId = UUID.randomUUID().toString()
        logger.error("Invoice service error [traceId: $traceId]: ${ex.message}", ex)

        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ErrorResponse(
                error = "Invoice service is currently unavailable. Please try again later.",
                traceId = traceId,
                timestamp = Instant.now()
            ))
    }
}

data class ErrorResponse(
    val error: String,
    val traceId: String,
    val timestamp: Instant
)