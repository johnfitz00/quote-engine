package com.example.quoteEngine.rating.infrastructure

import com.example.quoteEngine.quote.domain.Driver
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteRatingRequested
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.domain.Vehicle
import com.example.quoteEngine.quote.infrastructure.QuoteRepository
import com.example.quoteEngine.rating.domain.RatingRequest
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = ["quote.created", "quote.rating", "quote.rated", "quote.bound", "quote.expired"])
@TestPropertySource(properties = [
    "spring.kafka.bootstrap-servers=\${spring.embedded.kafka.brokers}",
    "spring.kafka.listener.auto-startup=true",
])
@DirtiesContext
class RatingConsumerIntegrationTest {
    @Autowired lateinit var quoteRepository: QuoteRepository

    @Autowired lateinit var kafkaTemplate: KafkaTemplate<String, String>

    @Autowired lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        quoteRepository.deleteAll()
    }

    @Test
    fun `quote transitions to RATED after QuoteRatingRequested is consumed`() {
        // Arrange — quote is already in RATING_IN_PROGRESS (set by the /rate endpoint)
        val quote =
            Quote().apply {
                status = QuoteStatus.RATING_IN_PROGRESS
                policyHolderName = "Alice Martin"
                state = "NSW"
                vehicle = Vehicle(year = 2020, make = "Toyota", model = "Corolla", annualKm = 15000)
                driver = Driver(age = 34, licenceYears = 12, atFaultClaims = 0)
            }
        quoteRepository.saveAndFlush(quote)
        val quoteId = quote.id!!

        val event =
            QuoteRatingRequested(
                quoteId = quoteId,
                ratingRequest =
                    RatingRequest(
                        vehicle = quote.vehicle!!,
                        driver = quote.driver!!,
                        state = "NSW",
                        effectiveDate = LocalDate.now(),
                    ),
            )

        // Act — publish the event as the /rate endpoint would
        kafkaTemplate.send("quote.rating", quoteId.toString(), objectMapper.writeValueAsString(event))

        // Assert — consumer processes it and transitions the quote within 5 seconds
        await().atMost(5, SECONDS).untilAsserted {
            val updated = quoteRepository.findById(quoteId).orElseThrow()
            assertEquals(QuoteStatus.RATED, updated.status)
            assertNotNull(updated.ratingResult)
            assertNotNull(updated.ratingResult!!.grossPremium)
        }
    }
}
