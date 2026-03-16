package com.example.quoteEngine.quote.infrastructure

import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface QuoteRepository : JpaRepository<Quote, UUID> {
    fun findByStatus(status: QuoteStatus): List<Quote>

    fun findByPolicyHolderNameContaining(name: String): List<Quote>
}
