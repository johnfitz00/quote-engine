package com.example.quoteEngine.quote.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class UpdateQuoteRequest(
    @NotBlank val policyHolderName: String,
    @Valid @NotNull var vehicle: VehicleRequest,
    @Valid @NotNull var driver: DriverRequest
)
