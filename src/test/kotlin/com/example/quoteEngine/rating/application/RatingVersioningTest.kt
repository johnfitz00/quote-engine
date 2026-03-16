package com.example.quoteEngine.rating.application

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Vehicle
import com.example.quoteEngine.rating.domain.RatingRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests that verify rate versioning against the real seeded rate table.
 * RatingDataLoader seeds two versions from motor-rates.yml on startup:
 *   Version 1 — effectiveFrom 2024-01-01, effectiveTo 2024-12-31
 *   Version 2 — effectiveFrom 2025-01-01, no expiry (open-ended)
 *
 * Risk profile chosen to isolate the AGE factor as the sole version-dependent variable:
 *   - Driver age 19  → band 17-24  → v1=1.85, v2=1.90
 *   - Vehicle year 2020, date 2024 → vehicleAge=4, date 2025 → vehicleAge=5 — both in band 4-8 → 1.00 in both versions
 *   - annualKm 15000   → band 10001-20000  → 1.00 in both versions
 *   - atFaultClaims 0  → band 0-0          → 1.00 in both versions
 *   - licenceYears 0   → NCB band 0-0      → 1.00 in both versions
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RatingVersioningTest {

    @Autowired
    lateinit var engine: RatingEngine

    private fun requestFor(effectiveDate: LocalDate) = RatingRequest(
        vehicle = Vehicle(year = 2020, make = "Toyota", model = "Camry", annualKm = 15000),
        driver  = Driver(age = 19, licenceYears = 0, atFaultClaims = 0),
        state   = "NSW",
        effectiveDate = effectiveDate,
    )

    @Test
    fun `effectiveDate in 2024 uses rate version 1 - AGE factor is 1_85`() {
        val result = engine.calculatePremium(requestFor(LocalDate.of(2024, 1, 1)))

        val ageFactor = result.factors.first { it.name == "AGE" }.factor

        assertEquals(ageFactor.compareTo(BigDecimal("1.85")), 0, "Expected AGE factor 1.85 (version 1), got $ageFactor")
    }

    @Test
    fun `effectiveDate in 2025 uses rate version 2 - AGE factor is 1_90`() {
        val result = engine.calculatePremium(requestFor(LocalDate.of(2025, 1, 1)))

        val ageFactor = result.factors.first { it.name == "AGE" }.factor

        assertEquals(ageFactor.compareTo(BigDecimal("1.90")), 0, "Expected AGE factor 1.90 (version 2), got $ageFactor")
    }

    @Test
    fun `version 2 produces a higher gross premium than version 1 for the same risk`() {
        val v1 = engine.calculatePremium(requestFor(LocalDate.of(2024, 6, 1)))
        val v2 = engine.calculatePremium(requestFor(LocalDate.of(2025, 6, 1)))

        assertTrue(
            v2.grossPremium.amount > v1.grossPremium.amount,
            "Expected v2 gross ${v2.grossPremium.amount} > v1 gross ${v1.grossPremium.amount}")
    }
}
