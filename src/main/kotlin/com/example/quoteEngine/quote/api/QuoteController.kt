package com.example.quoteEngine.quote.api

import com.example.quoteEngine.quote.api.dto.CreateQuoteRequest
import com.example.quoteEngine.quote.api.dto.QuoteResponse
import com.example.quoteEngine.quote.api.dto.UpdateQuoteRequest
import com.example.quoteEngine.quote.application.QuoteService
import com.example.quoteEngine.quote.domain.QuoteStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.util.UriComponentsBuilder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/v1/quotes")
class QuoteController(private val quoteService: QuoteService) {

    @PostMapping
    fun create(
        @Valid @RequestBody request: CreateQuoteRequest,
        uriBuilder: UriComponentsBuilder
    ): ResponseEntity<QuoteResponse> {
        val quote = quoteService.createQuote(request)
        val location = uriBuilder.path("/v1/quotes/{id}").buildAndExpand(quote.id).toUri()
        return ResponseEntity.created(location).body(quote)
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ResponseEntity<QuoteResponse> =
        ResponseEntity.ok(quoteService.getQuote(id))

    @GetMapping
    fun list(@RequestParam(required = false) status: QuoteStatus?): ResponseEntity<List<QuoteResponse>> =
        ResponseEntity.ok(quoteService.listQuotes(status))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateQuoteRequest
    ): ResponseEntity<QuoteResponse> =
        ResponseEntity.ok(quoteService.updateQuote(id, request))

    @PostMapping("/{id}/rate")
    fun rate(
        @PathVariable id: UUID,
        uriBuilder: UriComponentsBuilder
    ): ResponseEntity<QuoteResponse> {
        val response = quoteService.rateQuote(id)
        val location = uriBuilder.path("/v1/quotes/{id}").buildAndExpand(id).toUri()
        return ResponseEntity.accepted().location(location).body(response)
    }

    @PostMapping("/{id}/bind")
    fun bind(@PathVariable id: UUID): ResponseEntity<QuoteResponse> =
        ResponseEntity.ok(quoteService.bindQuote(id))

    @DeleteMapping("/{id}")
    fun decline(@PathVariable id: UUID): ResponseEntity<Void> {
        quoteService.decline(id)
        return ResponseEntity.noContent().build()
    }
}
