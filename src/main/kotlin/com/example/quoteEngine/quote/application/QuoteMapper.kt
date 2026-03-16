package com.example.quoteEngine.quote.application

import com.example.quoteEngine.quote.api.dto.AppliedFactorResponse
import com.example.quoteEngine.quote.api.dto.CreateQuoteRequest
import com.example.quoteEngine.quote.api.dto.DriverRequest
import com.example.quoteEngine.quote.api.dto.DriverResponse
import com.example.quoteEngine.quote.api.dto.MoneyResponse
import com.example.quoteEngine.quote.api.dto.QuoteResponse
import com.example.quoteEngine.quote.api.dto.RatingResultResponse
import com.example.quoteEngine.quote.api.dto.UpdateQuoteRequest
import com.example.quoteEngine.quote.api.dto.VehicleRequest
import com.example.quoteEngine.quote.api.dto.VehicleResponse
import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.domain.Vehicle
import com.example.quoteEngine.rating.domain.AppliedFactor
import com.example.quoteEngine.rating.domain.RatingResult
import com.example.quoteEngine.shared.domain.Money
import org.springframework.stereotype.Component

@Component
class QuoteMapper {
    fun toDomain(request: CreateQuoteRequest): Quote =
        Quote().apply {
            policyHolderName = request.policyHolderName
            state = request.state
            vehicle = request.vehicle.toDomain()
            driver = request.driver.toDomain()
            status = QuoteStatus.DRAFT
        }

    fun applyUpdate(
        request: UpdateQuoteRequest,
        quote: Quote,
    ) {
        quote.policyHolderName = request.policyHolderName
        quote.state = request.state
        quote.vehicle = request.vehicle.toDomain()
        quote.driver = request.driver.toDomain()
    }

    fun toResponse(quote: Quote): QuoteResponse =
        QuoteResponse(
            id = quote.id!!,
            policyHolderName = quote.policyHolderName!!,
            state = quote.state,
            vehicle = quote.vehicle!!.toResponse(),
            driver = quote.driver!!.toResponse(),
            status = quote.status.name,
            ratingResult = quote.ratingResult?.toResponse(),
            createdAt = quote.createdAt!!,
            updatedAt = quote.updatedAt!!,
        )

    private fun VehicleRequest.toDomain() = Vehicle(year = year, make = make, model = model, annualKm = annualKm)

    private fun DriverRequest.toDomain() = Driver(age = age, licenceYears = licenceYears, atFaultClaims = atFaultClaims)

    private fun Vehicle.toResponse() = VehicleResponse(year = year, make = make, model = model, annualKm = annualKm)

    private fun Driver.toResponse() =
        DriverResponse(age = age, licenceYears = licenceYears, atFaultClaims = atFaultClaims)

    private fun RatingResult.toResponse() =
        RatingResultResponse(
            basePremium = basePremium.toResponse(),
            factors = factors.map { it.toResponse() },
            technicalPremium = technicalPremium.toResponse(),
            levies = levies.toResponse(),
            stampDuty = stampDuty.toResponse(),
            grossPremium = grossPremium.toResponse(),
        )

    private fun AppliedFactor.toResponse() =
        AppliedFactorResponse(
            name = name,
            factor = factor,
            impactAmount = impactAmount.toResponse(),
        )

    private fun Money.toResponse() = MoneyResponse(amount = amount, currency = currency)
}
