package com.example.quoteEngine.rating.application

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Vehicle
import com.example.quoteEngine.rating.domain.FactorType
import com.example.quoteEngine.rating.domain.RatingRequest
import com.example.quoteEngine.rating.domain.RatingTable
import com.example.quoteEngine.rating.infrastructure.RatingDataLoader
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RatingEngineTest {

    private val rateLoader = mockk<RatingDataLoader>()
    private val engine = RatingEngine(rateLoader)

    private val baseVehicle = Vehicle(year = 2020, make = "Toyota", model = "Camry", annualKm = 15000)
    private val baseDate = LocalDate.of(2025, 6, 1)

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun row(factor: String) = RatingTable(
        factorType    = FactorType.AGE,   // type field unused in assertions; any value works
        bandStart     = BigDecimal.ZERO,
        bandEnd       = BigDecimal("999"),
        factorValue   = BigDecimal(factor),
        effectiveFrom = baseDate,
    )

    /** Stubs all five factor lookups; override individual factors under test. */
    private fun stubFactors(
        age: String        = "1.00",
        vehicleAge: String = "1.00",
        usage: String      = "1.00",
        claims: String     = "1.00",
        ncb: String        = "1.00",
    ) {
        every { rateLoader.findApplicable(FactorType.AGE,         any(), any()) } returns listOf(row(age))
        every { rateLoader.findApplicable(FactorType.VEHICLE_AGE, any(), any()) } returns listOf(row(vehicleAge))
        every { rateLoader.findApplicable(FactorType.USAGE,       any(), any()) } returns listOf(row(usage))
        every { rateLoader.findApplicable(FactorType.CLAIMS,      any(), any()) } returns listOf(row(claims))
        every { rateLoader.findApplicable(FactorType.NCB,         any(), any()) } returns listOf(row(ncb))
    }

    private fun request(
        age: Int          = 35,
        licenceYears: Int = 10,
        claims: Int       = 0,
        annualKm: Int     = 15000,
    ) = RatingRequest(
        vehicle = baseVehicle.copy(annualKm = annualKm),
        driver  = Driver(age = age, licenceYears = licenceYears, atFaultClaims = claims),
        state   = "NSW",
        effectiveDate = baseDate,
    )

    // ── Tests ─────────────────────────────────────────────────────────────

    @Test
    fun `young driver (age 19) pays more than experienced driver (age 45)`() {
        stubFactors(age = "1.90")
        val youngPremium = engine.calculatePremium(request(age = 19)).grossPremium.amount

        stubFactors(age = "0.90")
        val experiencedPremium = engine.calculatePremium(request(age = 45)).grossPremium.amount

        assertTrue(youngPremium.compareTo(experiencedPremium) > 0,
            "Expected young driver $youngPremium > experienced $experiencedPremium")
    }

    @Test
    fun `multiple at-fault claims increase gross premium`() {
        stubFactors(claims = "1.00")
        val cleanRecord = engine.calculatePremium(request(claims = 0)).grossPremium.amount

        stubFactors(claims = "1.78")
        val twoClaims = engine.calculatePremium(request(claims = 2)).grossPremium.amount

        assertTrue(twoClaims.compareTo(cleanRecord) > 0,
            "Expected two-claims $twoClaims > clean record $cleanRecord")
    }

    @Test
    fun `high annual km usage increases gross premium`() {
        stubFactors(usage = "1.00")
        val lowUsage = engine.calculatePremium(request(annualKm = 12000)).grossPremium.amount

        stubFactors(usage = "1.40")
        val highUsage = engine.calculatePremium(request(annualKm = 35000)).grossPremium.amount

        assertTrue(highUsage.compareTo(lowUsage) > 0,
            "Expected high-usage $highUsage > low-usage $lowUsage")
    }

    @Test
    fun `NCB discount reduces gross premium`() {
        stubFactors(ncb = "1.00")
        val noBonus = engine.calculatePremium(request()).grossPremium.amount

        stubFactors(ncb = "0.60")
        val maxBonus = engine.calculatePremium(request(licenceYears = 10)).grossPremium.amount

        assertTrue(maxBonus.compareTo(noBonus) < 0,
            "Expected max-NCB $maxBonus < no-NCB $noBonus")
    }

    @Test
    fun `result contains one applied factor per factor type`() {
        stubFactors()
        val result = engine.calculatePremium(request())

        assertEquals(5, result.factors.size)
        assertTrue(result.factors.map { it.name }.containsAll(
            listOf("AGE", "VEHICLE_AGE", "USAGE", "CLAIMS", "NCB")
        ))
    }

    @Test
    fun `stamp duty is calculated as percentage of technical premium`() {
        // All factors neutral → technical premium equals base (500.00); NSW rate = 9%
        stubFactors()
        val result = engine.calculatePremium(request())

        assertTrue(result.stampDuty.amount.compareTo(BigDecimal("45.00")) == 0,
            "Expected NSW stamp duty 45.00 (9% of 500.00), got ${result.stampDuty.amount}")
    }

    @Test
    fun `minimum age 17 is rated without error`() {
        stubFactors(age = "1.95")   // young driver band
        val result = engine.calculatePremium(request(age = 17))

        assertTrue(result.grossPremium.amount.compareTo(BigDecimal.ZERO) > 0,
            "Expected positive premium for minimum-age driver")
    }

    @Test
    fun `new vehicle (age 0-3) applies correct vehicle age factor`() {
        stubFactors(vehicleAge = "1.25")
        val result = engine.calculatePremium(request())

        val vehicleAgeFactor = result.factors.first { it.name == "VEHICLE_AGE" }.factor
        assertTrue(vehicleAgeFactor.compareTo(BigDecimal("1.25")) == 0,
            "Expected VEHICLE_AGE factor 1.25, got $vehicleAgeFactor")
    }

    @Test
    fun `zero annual km falls in lowest usage bracket`() {
        stubFactors(usage = "0.88")
        val result = engine.calculatePremium(request(annualKm = 0))

        val usageFactor = result.factors.first { it.name == "USAGE" }.factor
        assertTrue(usageFactor.compareTo(BigDecimal("0.88")) == 0,
            "Expected USAGE factor 0.88 for zero km, got $usageFactor")
    }

    @Test
    fun `three or more at-fault claims apply maximum claims factor`() {
        stubFactors(claims = "2.10")
        val highRisk = engine.calculatePremium(request(claims = 3)).grossPremium.amount

        stubFactors(claims = "1.00")
        val noRisk = engine.calculatePremium(request(claims = 0)).grossPremium.amount

        assertTrue(highRisk.compareTo(noRisk) > 0,
            "Expected 3-claim premium $highRisk > no-claim $noRisk")
    }
}
