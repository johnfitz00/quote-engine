package com.example.quoteEngine.quote.api.dto

import java.time.Instant
import java.util.UUID

data class QuoteResponse(
    val id: UUID,
    val policyHolderName: String,
    val vehicle: VehicleResponse,
    val driver: DriverResponse,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class VehicleResponse(
    val year: Int,
    val make: String,
    val model: String,
    val annualKm: Int
)

data class DriverResponse(
    val age: Int,
    val licenceYears: Int,
    val atFaultClaims: Int
)
