package com.example.quoteEngine.quote.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QuoteEventRepository : JpaRepository<QuoteEvent, UUID> {
    fun findByQuoteIdOrderByOccurredAtAsc(quoteId: UUID): List<QuoteEvent>
}
