package com.ahold.technl.sandbox.exception

class DeliveryNotFoundException(message: String) : RuntimeException(message)

class InvalidDeliveryStateException(message: String) : RuntimeException(message)

class InvoiceServiceException(message: String) : RuntimeException(message)


