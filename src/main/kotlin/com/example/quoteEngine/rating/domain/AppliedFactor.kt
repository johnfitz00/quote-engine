package com.example.quoteEngine.rating.domain

import com.example.quoteEngine.shared.domain.Money
import java.math.BigDecimal

data class AppliedFactor(
    val name: String,
    val factor: BigDecimal,
    val impactAmount: Money
)
