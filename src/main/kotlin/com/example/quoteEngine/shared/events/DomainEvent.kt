package com.example.quoteEngine.shared.events

import java.time.Instant
import java.util.UUID

abstract class DomainEvent(
    val aggregateId: UUID,
    val aggregateType: String,
    val eventType: String,
    val eventId: UUID = UUID.randomUUID(),
    val occurredAt: Instant = Instant.now(),
) {
    abstract val topic: String
}
