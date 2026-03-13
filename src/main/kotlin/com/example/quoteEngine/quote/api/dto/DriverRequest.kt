package com.example.quoteEngine.quote.api.dto

import jakarta.validation.constraints.Min

data class DriverRequest(
    @Min(16) val age: Int,
    @Min(0) val licenceYears: Int,
    @Min(0) val atFaultClaims: Int
)
