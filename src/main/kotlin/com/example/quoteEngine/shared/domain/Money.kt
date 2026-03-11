package com.example.quoteEngine.shared.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class Money(
    val amount: BigDecimal,
    val currency: String
) {
    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amount = amount + other.amount)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return copy(amount = amount - other.amount)
    }

    fun multiply(factor: BigDecimal): Money =
        copy(amount = amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN))

    companion object {
        fun of(amount: String, currency: String): Money =
            Money(BigDecimal(amount).setScale(2, RoundingMode.HALF_EVEN), currency)
    }
}
