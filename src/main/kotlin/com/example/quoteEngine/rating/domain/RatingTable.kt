package com.example.quoteEngine.rating.domain

import java.math.BigDecimal
import java.time.LocalDate

data class RatingTable(
    val factorType: FactorType,
    val bandStart: BigDecimal,
    val bandEnd: BigDecimal,
    val factorValue: BigDecimal,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate? = null,
)
