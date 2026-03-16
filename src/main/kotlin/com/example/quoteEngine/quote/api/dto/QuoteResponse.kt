package com.example.quoteEngine.quote.api.dto

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class QuoteResponse(
    val id: UUID,
    val policyHolderName: String,
    val state: String?,
    val vehicle: VehicleResponse,
    val driver: DriverResponse,
    val status: String,
    val ratingResult: RatingResultResponse?,
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

data class RatingResultResponse(
    val basePremium: MoneyResponse,
    val factors: List<AppliedFactorResponse>,
    val technicalPremium: MoneyResponse,
    val levies: MoneyResponse,
    val stampDuty: MoneyResponse,
    val grossPremium: MoneyResponse
)

data class AppliedFactorResponse(
    val name: String,
    val factor: BigDecimal,
    val impactAmount: MoneyResponse
)

data class MoneyResponse(
    val amount: BigDecimal,
    val currency: String
)
