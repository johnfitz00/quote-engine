package com.example.quoteEngine.quote.domain

class InvalidTransitionException : RuntimeException {
    constructor(from: QuoteStatus, to: QuoteStatus) : super("Cannot transition quote from $from to $to")
    constructor(message: String) : super(message)
}
