package com.example.quoteEngine.quote.domain

import com.example.quoteEngine.shared.events.DomainEvent
import java.util.UUID

data class QuoteCreated(
    val quoteId: UUID,
    val policyHolderName: String?,
    val state: String?,
    val vehicle: Vehicle,
    val driver: Driver
) : DomainEvent(
    aggregateId = quoteId,
    aggregateType = "Quote",
    eventType = "QuoteCreated"
) {
    override val topic = "quote.created"
}
