package com.example.quoteEngine.rating.domain

import com.example.quoteEngine.shared.domain.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.AttributeOverrides
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Transient

@Embeddable
data class RatingResult(
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "rating_base_amount")),
        AttributeOverride(name = "currency", column = Column(name = "rating_base_currency")),
    )
    val basePremium: Money = Money.of("0.00", "AUD"),
    // Not persisted — populated by RatingEngine at rating time, empty on DB reload
    @Transient
    val factors: List<AppliedFactor> = emptyList(),
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "rating_technical_amount")),
        AttributeOverride(name = "currency", column = Column(name = "rating_technical_currency")),
    )
    val technicalPremium: Money = Money.of("0.00", "AUD"),
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "rating_levies_amount")),
        AttributeOverride(name = "currency", column = Column(name = "rating_levies_currency")),
    )
    val levies: Money = Money.of("0.00", "AUD"),
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "rating_stamp_duty_amount")),
        AttributeOverride(name = "currency", column = Column(name = "rating_stamp_duty_currency")),
    )
    val stampDuty: Money = Money.of("0.00", "AUD"),
    @Embedded
    @AttributeOverrides(
        AttributeOverride(name = "amount", column = Column(name = "rating_gross_amount")),
        AttributeOverride(name = "currency", column = Column(name = "rating_gross_currency")),
    )
    val grossPremium: Money = Money.of("0.00", "AUD"),
)
