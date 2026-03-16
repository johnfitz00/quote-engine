package com.example.quoteEngine.quote.infrastructure

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.domain.Vehicle
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

@DataJpaTest
class QuoteRepositoryTest {
    @Autowired
    lateinit var quoteRepository: QuoteRepository

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Test
    fun `findByStatus returns only matching quotes`() {
        quoteRepository.save(quote("Alice Martin", QuoteStatus.DRAFT))
        quoteRepository.save(quote("Bob Henderson", QuoteStatus.RATED))
        quoteRepository.save(quote("Carol Nguyen", QuoteStatus.RATED))

        val results = quoteRepository.findByStatus(QuoteStatus.RATED)

        assertThat(results).hasSize(2)
        assertThat(results).allMatch { it.status == QuoteStatus.RATED }
    }

    @Test
    fun `saved quote can be retrieved with all fields intact`() {
        val saved = quoteRepository.saveAndFlush(quote("Alice Martin", QuoteStatus.DRAFT))
        entityManager.clear()

        val found = quoteRepository.findById(saved.id!!).orElseThrow()

        assertThat(found.policyHolderName).isEqualTo("Alice Martin")
        assertThat(found.status).isEqualTo(QuoteStatus.DRAFT)
        assertThat(found.vehicle?.make).isEqualTo("Toyota")
        assertThat(found.vehicle?.model).isEqualTo("Corolla")
        assertThat(found.vehicle?.year).isEqualTo(2020)
        assertThat(found.vehicle?.annualKm).isEqualTo(15000)
        assertThat(found.driver?.age).isEqualTo(34)
        assertThat(found.driver?.licenceYears).isEqualTo(12)
        assertThat(found.driver?.atFaultClaims).isEqualTo(0)
        assertThat(found.createdAt).isNotNull()
        assertThat(found.updatedAt).isNotNull()
    }

    private fun quote(
        name: String,
        status: QuoteStatus,
    ) = Quote().apply {
        policyHolderName = name
        vehicle = Vehicle(year = 2020, make = "Toyota", model = "Corolla", annualKm = 15000)
        driver = Driver(age = 34, licenceYears = 12, atFaultClaims = 0)
        this.status = status
    }
}
