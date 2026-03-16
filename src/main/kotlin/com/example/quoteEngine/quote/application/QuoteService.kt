package com.example.quoteEngine.quote.application

import com.example.quoteEngine.quote.api.dto.CreateQuoteRequest
import com.example.quoteEngine.quote.api.dto.QuoteResponse
import com.example.quoteEngine.quote.api.dto.UpdateQuoteRequest
import com.example.quoteEngine.quote.domain.InvalidTransitionException
import com.example.quoteEngine.quote.domain.Quote
import com.example.quoteEngine.quote.domain.QuoteNotFoundException
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.example.quoteEngine.quote.infrastructure.QuoteRepository
import com.example.quoteEngine.rating.application.RatingEngine
import com.example.quoteEngine.rating.domain.RatingRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class QuoteService(
    private val quoteRepository: QuoteRepository,
    private val quoteMapper: QuoteMapper,
    private val ratingEngine: RatingEngine,
) {

    @Transactional
    fun createQuote(request: CreateQuoteRequest): QuoteResponse {
        val quote = quoteMapper.toDomain(request)
        return quoteMapper.toResponse(quoteRepository.saveAndFlush(quote))
    }

    @Transactional(readOnly = true)
    fun getQuote(id: UUID): QuoteResponse {
        val quote = quoteRepository.findById(id)
            .orElseThrow { QuoteNotFoundException(id) }
        return quoteMapper.toResponse(quote)
    }

    @Transactional(readOnly = true)
    fun listQuotes(status: QuoteStatus?): List<QuoteResponse> {
        val quotes = if (status != null) quoteRepository.findByStatus(status)
                     else quoteRepository.findAll()
        return quotes.map { quoteMapper.toResponse(it) }
    }

    @Transactional
    fun updateQuote(id: UUID, request: UpdateQuoteRequest): QuoteResponse {
        val quote = quoteRepository.findById(id)
            .orElseThrow { QuoteNotFoundException(id) }
        if (quote.status != QuoteStatus.DRAFT) {
            throw InvalidTransitionException("Cannot edit a ${quote.status} quote — only DRAFT quotes can be updated")
        }
        quoteMapper.applyUpdate(request, quote)
        return quoteMapper.toResponse(quoteRepository.saveAndFlush(quote))
    }

    @Transactional
    fun rateQuote(id: UUID): QuoteResponse {
        val quote = quoteRepository.findById(id).orElseThrow { QuoteNotFoundException(id) }
        if (!quote.status.canTransitionTo(QuoteStatus.RATED)) {
            throw InvalidTransitionException(quote.status, QuoteStatus.RATED)
        }
        val state = quote.state
            ?: throw IllegalStateException("Quote $id has no state — update the quote before rating")
        val request = RatingRequest(
            vehicle = quote.vehicle!!,
            driver = quote.driver!!,
            state = state,
            effectiveDate = LocalDate.now()
        )
        quote.ratingResult = ratingEngine.calculatePremium(request)
        quote.status = QuoteStatus.RATED
        return quoteMapper.toResponse(quoteRepository.saveAndFlush(quote))
    }

    @Transactional
    fun bindQuote(id: UUID): QuoteResponse =
        quoteMapper.toResponse(transitionTo(id, QuoteStatus.BOUND))

    @Transactional
    fun decline(id: UUID) {
        transitionTo(id, QuoteStatus.DECLINED)
    }

    private fun transitionTo(id: UUID, next: QuoteStatus): Quote {
        val quote = quoteRepository.findById(id)
            .orElseThrow { QuoteNotFoundException(id) }
        if (!quote.status.canTransitionTo(next)) {
            throw InvalidTransitionException(quote.status, next)
        }
        quote.status = next
        return quoteRepository.saveAndFlush(quote)
    }
}
