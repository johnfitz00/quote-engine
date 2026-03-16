package com.example.quoteEngine.rating.application

import com.example.quoteEngine.rating.domain.AppliedFactor
import com.example.quoteEngine.rating.domain.FactorType
import com.example.quoteEngine.rating.domain.RatingException
import com.example.quoteEngine.rating.domain.RatingRequest
import com.example.quoteEngine.rating.domain.RatingResult
import com.example.quoteEngine.rating.infrastructure.RatingDataLoader
import com.example.quoteEngine.shared.domain.Money
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class RatingEngine(private val rateLoader: RatingDataLoader) {

    companion object {
        // Flat base premium — in a real system this would be derived from sum insured or vehicle value
        private val BASE_PREMIUM = Money.of("500.00", "AUD")

        // Fire and theft levy: flat amount added after technical premium
        private val FIRE_AND_THEFT_LEVY = Money.of("25.00", "AUD")

        // Stamp duty rates by Australian state/territory (applied to technical premium)
        private val STAMP_DUTY_RATES = mapOf(
            "NSW" to BigDecimal("0.09"),
            "VIC" to BigDecimal("0.10"),
            "QLD" to BigDecimal("0.09"),
            "SA"  to BigDecimal("0.11"),
            "WA"  to BigDecimal("0.10"),
            "TAS" to BigDecimal("0.08"),
            "NT"  to BigDecimal("0.10"),
            "ACT" to BigDecimal("0.08"),
        )
    }

    fun calculatePremium(request: RatingRequest): RatingResult {
        val date       = request.effectiveDate
        val vehicle    = request.vehicle
        val driver     = request.driver
        val vehicleAge = date.year - vehicle.year

        // Ordered factor inputs — sequence matters: NCB discount applied last
        val factorInputs = listOf(
            FactorType.AGE         to driver.age.toBigDecimal(),
            FactorType.VEHICLE_AGE to vehicleAge.toBigDecimal(),
            FactorType.USAGE       to vehicle.annualKm.toBigDecimal(),
            FactorType.CLAIMS      to driver.atFaultClaims.toBigDecimal(),
            FactorType.NCB         to driver.licenceYears.toBigDecimal(),
        )

        var running = BASE_PREMIUM
        val appliedFactors = mutableListOf<AppliedFactor>()

        for ((type, value) in factorInputs) {
            val row = rateLoader.findApplicable(type, value, date).singleOrNull()
                ?: throw RatingException(
                    "No rating band found for factor $type with value $value on $date — check rate table coverage"
                )
            val before = running
            running = running.multiply(row.factorValue)
            appliedFactors += AppliedFactor(
                name         = type.name,
                factor       = row.factorValue,
                impactAmount = running - before,
            )
        }

        val technicalPremium = running
        val stampDutyRate    = STAMP_DUTY_RATES[request.state.uppercase()]
            ?: throw RatingException(
                "Unknown state '${request.state}' — add it to the stamp duty rate table"
            )
        val stampDuty    = technicalPremium.multiply(stampDutyRate)
        // Final gross rounds with HALF_UP — regulatory convention for consumer-facing premiums
        val grossPremium = (technicalPremium + FIRE_AND_THEFT_LEVY + stampDuty).round()

        return RatingResult(
            basePremium      = BASE_PREMIUM,
            factors          = appliedFactors,
            technicalPremium = technicalPremium,
            levies           = FIRE_AND_THEFT_LEVY,
            stampDuty        = stampDuty,
            grossPremium     = grossPremium,
        )
    }
}
