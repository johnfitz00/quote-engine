package com.example.quoteEngine.quote.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuoteStatusTest {

    // DRAFT transitions
    @Test
    fun `DRAFT can transition to RATING_IN_PROGRESS`() {
        assertTrue(QuoteStatus.DRAFT.canTransitionTo(QuoteStatus.RATING_IN_PROGRESS))
    }

    @Test
    fun `DRAFT can transition to DECLINED`() {
        assertTrue(QuoteStatus.DRAFT.canTransitionTo(QuoteStatus.DECLINED))
    }

    @Test
    fun `DRAFT cannot transition to BOUND`() {
        assertFalse(QuoteStatus.DRAFT.canTransitionTo(QuoteStatus.BOUND))
    }

    @Test
    fun `DRAFT cannot transition to REFERRED`() {
        assertFalse(QuoteStatus.DRAFT.canTransitionTo(QuoteStatus.REFERRED))
    }

    @Test
    fun `DRAFT cannot transition to EXPIRED`() {
        assertFalse(QuoteStatus.DRAFT.canTransitionTo(QuoteStatus.EXPIRED))
    }

    // RATED transitions
    @Test
    fun `RATED can transition to BOUND`() {
        assertTrue(QuoteStatus.RATED.canTransitionTo(QuoteStatus.BOUND))
    }

    @Test
    fun `RATED can transition to REFERRED`() {
        assertTrue(QuoteStatus.RATED.canTransitionTo(QuoteStatus.REFERRED))
    }

    @Test
    fun `RATED cannot transition to DRAFT`() {
        assertFalse(QuoteStatus.RATED.canTransitionTo(QuoteStatus.DRAFT))
    }

    // Terminal states
    @Test
    fun `BOUND cannot transition to any status`() {
        QuoteStatus.entries.forEach { next ->
            assertFalse(QuoteStatus.BOUND.canTransitionTo(next), "BOUND should not transition to $next")
        }
    }

    @Test
    fun `DECLINED cannot transition to any status`() {
        QuoteStatus.entries.forEach { next ->
            assertFalse(QuoteStatus.DECLINED.canTransitionTo(next), "DECLINED should not transition to $next")
        }
    }

    @Test
    fun `EXPIRED cannot transition to any status`() {
        QuoteStatus.entries.forEach { next ->
            assertFalse(QuoteStatus.EXPIRED.canTransitionTo(next), "EXPIRED should not transition to $next")
        }
    }
}
