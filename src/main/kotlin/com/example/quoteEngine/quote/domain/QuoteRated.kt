package com.example.quoteEngine.quote.domain

import com.example.quoteEngine.rating.domain.RatingResult
import com.example.quoteEngine.shared.events.DomainEvent
import java.util.UUID

data class QuoteRated(
    val quoteId: UUID,
    val ratingResult: RatingResult
) : DomainEvent(
    aggregateId = quoteId,
    aggregateType = "Quote",
    eventType = "QuoteRated"
) {
    override val topic = "quote.rated"
}
