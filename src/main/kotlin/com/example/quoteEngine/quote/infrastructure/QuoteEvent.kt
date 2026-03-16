package com.example.quoteEngine.quote.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "quote_events")
class QuoteEvent(
    @Id
    val id: UUID = UUID.randomUUID(),
    val quoteId: UUID,
    val eventType: String,
    @Column(columnDefinition = "TEXT")
    val eventData: String,
    val occurredAt: Instant,
)
