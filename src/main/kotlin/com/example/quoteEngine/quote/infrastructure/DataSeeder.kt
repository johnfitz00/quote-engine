package com.example.quoteEngine.quote.infrastructure

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.domain.Vehicle
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class DataSeeder(
    private val quoteRepository: QuoteRepository,
) : ApplicationRunner {
    companion object {
        private val log = LoggerFactory.getLogger(DataSeeder::class.java)
    }

    override fun run(args: ApplicationArguments) {
        log.info("DataSeeder running...")
        if (quoteRepository.count() > 0) {
            log.info("Data already exists, skipping seed")
            return
        }

        val draft =
            Quote().apply {
                policyHolderName = "Alice Martin"
                vehicle = Vehicle(year = 2020, make = "Toyota", model = "Corolla", annualKm = 15000)
                driver = Driver(age = 34, licenceYears = 12, atFaultClaims = 0)
                status = QuoteStatus.DRAFT
            }

        val rated =
            Quote().apply {
                policyHolderName = "Bob Henderson"
                vehicle = Vehicle(year = 2018, make = "Honda", model = "Civic", annualKm = 22000)
                driver = Driver(age = 27, licenceYears = 5, atFaultClaims = 1)
                status = QuoteStatus.RATED
            }

        val bound =
            Quote().apply {
                policyHolderName = "Carol Nguyen"
                vehicle = Vehicle(year = 2022, make = "Ford", model = "Escape", annualKm = 18000)
                driver = Driver(age = 45, licenceYears = 24, atFaultClaims = 0)
                status = QuoteStatus.BOUND
            }

        quoteRepository.saveAll(listOf(draft, rated, bound))
        log.info("Seeded 3 sample quotes")
    }
}
