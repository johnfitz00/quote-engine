package com.example.quoteEngine.rating.application

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Vehicle
import com.example.quoteEngine.rating.domain.FactorType
import com.example.quoteEngine.rating.domain.RatingRequest
import com.example.quoteEngine.rating.domain.RatingTable
import com.example.quoteEngine.rating.infrastructure.RatingDataLoader
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.forAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

class RatingEnginePropertyTest {

    private val rateLoader = mockk<RatingDataLoader>()
    private val engine = RatingEngine(rateLoader)

    private val effectiveDate = LocalDate.of(2025, 6, 1)

    private fun stubAllWith(factor: BigDecimal) {
        FactorType.entries.forEach { type ->
            every { rateLoader.findApplicable(type, any(), any()) } returns
                listOf(RatingTable(
                    factorType    = type,
                    bandStart     = BigDecimal.ZERO,
                    bandEnd       = BigDecimal("999"),
                    factorValue   = factor,
                    effectiveFrom = effectiveDate,
                ))
        }
    }

    private fun request(age: Int, vehicleYear: Int) = RatingRequest(
        vehicle = Vehicle(year = vehicleYear, make = "Toyota", model = "Camry", annualKm = 15000),
        driver  = Driver(age = age, licenceYears = 5, atFaultClaims = 0),
        state   = "NSW",
        effectiveDate = effectiveDate,
    )

    @Test
    fun `rating invariants hold for any valid driver age and vehicle year`() {
        // Arb.int(50..300).map gives clean BigDecimal values 0.50–3.00 with no Arb.bigDecimal overload ambiguity
        val factorArb = Arb.int(50..300).map { BigDecimal(it).divide(BigDecimal("100"), 4, RoundingMode.HALF_EVEN) }

        runBlocking {
            forAll<Int, Int, BigDecimal>(
                PropTestConfig(),
                Arb.int(17..80),
                Arb.int(1990..2024),
                factorArb,
            ) { age, vehicleYear, factor ->
                stubAllWith(factor)
                val result = engine.calculatePremium(request(age, vehicleYear))
                val vehicleAge = effectiveDate.year - vehicleYear

                println(
                    "[${attempts().toString().padStart(2)}] " +
                    "driverAge=$age  vehicleAge=$vehicleAge  factor=$factor" +
                    "  →  technical=${result.technicalPremium.amount}  gross=${result.grossPremium.amount}"
                )

                // gross premium must be positive
                result.grossPremium.amount.compareTo(BigDecimal.ZERO) > 0 &&
                // levies and stamp duty sit on top of technical premium
                result.grossPremium.amount.compareTo(result.technicalPremium.amount) > 0 &&
                // every applied factor must be within the valid rate-table range
                result.factors.all {
                    it.factor.compareTo(BigDecimal("0.5")) >= 0 &&
                    it.factor.compareTo(BigDecimal("3.0")) <= 0
                }
            }
        }
    }
}
