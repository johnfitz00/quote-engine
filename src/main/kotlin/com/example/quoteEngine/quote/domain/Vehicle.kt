package com.example.quoteEngine.quote.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Vehicle(
    @Column(name = "vehicle_year")
    val year: Int,
    val make: String,
    val model: String,
    val annualKm: Int,
)
