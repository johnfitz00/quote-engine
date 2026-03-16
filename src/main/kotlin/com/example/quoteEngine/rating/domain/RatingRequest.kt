package com.example.quoteEngine.rating.domain

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Vehicle
import java.time.LocalDate

data class RatingRequest(
    val vehicle: Vehicle,
    val driver: Driver,
    val state: String,
    val effectiveDate: LocalDate
)
