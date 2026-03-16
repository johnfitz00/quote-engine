package com.example.quoteEngine.quote.domain

enum class QuoteStatus {
    DRAFT,
    RATING_IN_PROGRESS,
    RATED,
    REFERRED,
    BOUND,
    EXPIRED,
    DECLINED,
    ;

    val allowedTransitions: Set<QuoteStatus> by lazy {
        when (this) {
            DRAFT -> setOf(RATING_IN_PROGRESS, DECLINED)
            RATING_IN_PROGRESS -> setOf(RATED, DECLINED)
            RATED -> setOf(REFERRED, BOUND, DECLINED, EXPIRED)
            REFERRED -> setOf(BOUND, DECLINED, EXPIRED)
            BOUND, EXPIRED, DECLINED -> emptySet()
        }
    }

    fun canTransitionTo(next: QuoteStatus): Boolean = next in allowedTransitions
}
