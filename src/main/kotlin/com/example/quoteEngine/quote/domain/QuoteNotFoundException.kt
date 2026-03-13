package com.example.quoteEngine.quote.domain

import java.util.UUID

class QuoteNotFoundException(id: UUID) : RuntimeException("Quote $id not found")
