package com.example.quoteEngine.quote.domain

import com.example.quoteEngine.shared.events.DomainEvent
import java.util.UUID

data class QuoteExpired(
    val quoteId: UUID,
    val finalStatus: QuoteStatus
) : DomainEvent(
    aggregateId = quoteId,
    aggregateType = "Quote",
    eventType = "QuoteExpired"
) {
    override val topic = "quote.expired"
}
