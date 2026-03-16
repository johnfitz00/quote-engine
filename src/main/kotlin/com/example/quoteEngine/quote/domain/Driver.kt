package com.example.quoteEngine.quote.domain

import jakarta.persistence.Embeddable

@Embeddable
data class Driver(
    val age: Int,
    val licenceYears: Int,
    val atFaultClaims: Int,
)
