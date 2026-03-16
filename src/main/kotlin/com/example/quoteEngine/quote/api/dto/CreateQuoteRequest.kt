package com.example.quoteEngine.quote.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateQuoteRequest(
    @NotBlank val policyHolderName: String,
    @NotBlank val state: String,
    @Valid @NotNull val vehicle: VehicleRequest,
    @Valid @NotNull val driver: DriverRequest
)
