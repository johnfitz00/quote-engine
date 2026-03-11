package com.example.quoteEngine.quote.domain

import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.Instant
import java.util.UUID

@Entity
class Quote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var policyHolderName: String? = null

    @Embedded
    var vehicle: Vehicle? = null

    @Embedded
    var driver: Driver? = null

    var status: QuoteStatus = QuoteStatus.DRAFT

    @CreationTimestamp
    var createdAt: Instant? = null
    @UpdateTimestamp
    var updatedAt: Instant? = null
}