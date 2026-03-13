package com.example.quoteEngine.quote.application

import com.example.quoteEngine.quote.api.dto.CreateQuoteRequest
import com.example.quoteEngine.quote.api.dto.DriverRequest
import com.example.quoteEngine.quote.api.dto.DriverResponse
import com.example.quoteEngine.quote.api.dto.QuoteResponse
import com.example.quoteEngine.quote.api.dto.UpdateQuoteRequest
import com.example.quoteEngine.quote.api.dto.VehicleRequest
import com.example.quoteEngine.quote.api.dto.VehicleResponse
import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.domain.Vehicle
import org.springframework.stereotype.Component

@Component
class QuoteMapper {

    fun toDomain(request: CreateQuoteRequest): Quote = Quote().apply {
        policyHolderName = request.policyHolderName
        vehicle = request.vehicle.toDomain()
        driver = request.driver.toDomain()
        status = QuoteStatus.DRAFT
    }

    fun applyUpdate(request: UpdateQuoteRequest, quote: Quote) {
        quote.policyHolderName = request.policyHolderName
        quote.vehicle = request.vehicle.toDomain()
        quote.driver = request.driver.toDomain()
    }

    fun toResponse(quote: Quote): QuoteResponse = QuoteResponse(
        id = quote.id!!,
        policyHolderName = quote.policyHolderName!!,
        vehicle = quote.vehicle!!.toResponse(),
        driver = quote.driver!!.toResponse(),
        status = quote.status.name,
        createdAt = quote.createdAt!!,
        updatedAt = quote.updatedAt!!
    )

    private fun VehicleRequest.toDomain() = Vehicle(year = year, make = make, model = model, annualKm = annualKm)
    private fun DriverRequest.toDomain() = Driver(age = age, licenceYears = licenceYears, atFaultClaims = atFaultClaims)
    private fun Vehicle.toResponse() = VehicleResponse(year = year, make = make, model = model, annualKm = annualKm)
    private fun Driver.toResponse() = DriverResponse(age = age, licenceYears = licenceYears, atFaultClaims = atFaultClaims)
}
