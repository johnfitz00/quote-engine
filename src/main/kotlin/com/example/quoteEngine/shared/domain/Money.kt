package com.example.quoteEngine.shared.domain

import jakarta.persistence.Embeddable
import java.math.BigDecimal
import java.math.RoundingMode

@Embeddable
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

    // Used for intermediate factor multiplications — banker's rounding avoids cumulative bias
    fun multiply(factor: BigDecimal): Money =
        copy(amount = amount.multiply(factor).setScale(2, RoundingMode.HALF_EVEN))

    // Used for the final consumer-facing premium — always rounds half-up (regulatory convention)
    fun round(): Money = copy(amount = amount.setScale(2, RoundingMode.HALF_UP))

    companion object {
        fun of(amount: String, currency: String): Money =
            Money(BigDecimal(amount).setScale(2, RoundingMode.HALF_EVEN), currency)
    }
}
