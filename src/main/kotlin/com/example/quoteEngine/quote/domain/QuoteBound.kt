package com.example.quoteEngine.quote.domain

import com.example.quoteEngine.shared.events.DomainEvent
import java.util.UUID

data class QuoteBound(
    val quoteId: UUID,
) : DomainEvent(
        aggregateId = quoteId,
        aggregateType = "Quote",
        eventType = "QuoteBound",
    ) {
    override val topic = "quote.bound"
}
