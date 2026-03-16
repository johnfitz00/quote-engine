package com.example.quoteEngine.quote.api.dto

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class VehicleRequest(
    @Min(1900) val year: Int,
    @NotBlank val make: String,
    @NotBlank val model: String,
    @Positive val annualKm: Int,
)
