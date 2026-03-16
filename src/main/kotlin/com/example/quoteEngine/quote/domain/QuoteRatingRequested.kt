package com.example.quoteEngine.quote.domain

import com.example.quoteEngine.rating.domain.RatingRequest
import com.example.quoteEngine.shared.events.DomainEvent
import java.util.UUID

data class QuoteRatingRequested(
    val quoteId: UUID,
    val ratingRequest: RatingRequest
) : DomainEvent(
    aggregateId = quoteId,
    aggregateType = "Quote",
    eventType = "QuoteRatingRequested"
) {
    override val topic = "quote.rating"
}
