package com.example.quoteEngine.rating.infrastructure

import com.example.quoteEngine.quote.domain.QuoteNotFoundException
import com.example.quoteEngine.quote.domain.QuoteRated
import com.example.quoteEngine.quote.domain.QuoteRatingRequested
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.infrastructure.QuoteEventPublisher
import com.example.quoteEngine.quote.infrastructure.QuoteRepository
import com.example.quoteEngine.rating.application.RatingEngine
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Component
class RatingConsumer(
    private val quoteRepository: QuoteRepository,
    private val ratingEngine: RatingEngine,
    private val eventPublisher: QuoteEventPublisher,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["quote.rating"])
    @Transactional
    fun consume(payload: String) {
        try {
            val event = objectMapper.readValue(payload, QuoteRatingRequested::class.java)
            val quoteId = event.aggregateId

            val quote = quoteRepository.findById(quoteId)
                .orElseThrow { QuoteNotFoundException(quoteId) }

            val ratingResult = ratingEngine.calculatePremium(event.ratingRequest)

            quote.ratingResult = ratingResult
            quote.status = QuoteStatus.RATED
            quoteRepository.saveAndFlush(quote)

            eventPublisher.publish(QuoteRated(quoteId, ratingResult))

            log.info("Quote {} rated successfully — gross premium {}", quoteId, ratingResult.grossPremium)
        } catch (e: Exception) {
            log.error("Failed to process QuoteRatingRequested — message skipped: {}", e.message, e)
        }
    }
}
