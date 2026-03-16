package com.example.quoteEngine.rating.infrastructure

import com.example.quoteEngine.rating.domain.FactorType
import com.example.quoteEngine.rating.domain.RatingTable
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.yaml.snakeyaml.Yaml
import java.math.BigDecimal
import java.time.LocalDate

@Component
class RatingDataLoader {

    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var rates: List<RatingTable>

    @PostConstruct
    fun load() {
        val resource = ClassPathResource("rates/motor-rates.yml")
        @Suppress("UNCHECKED_CAST")
        val root = Yaml().load<Map<String, Any>>(resource.inputStream)
        rates = buildEntries(root)
        log.info("Loaded ${rates.size} rate entries from motor-rates.yml")
    }

    fun findApplicable(type: FactorType, value: BigDecimal, date: LocalDate): List<RatingTable> =
        rates.filter { row ->
            row.factorType == type &&
            value >= row.bandStart && value <= row.bandEnd &&
            date >= row.effectiveFrom && (row.effectiveTo == null || date <= row.effectiveTo)
        }

    @Suppress("UNCHECKED_CAST")
    private fun buildEntries(root: Map<String, Any>): List<RatingTable> {
        val versions = root["versions"] as List<Map<String, Any>>
        return versions.flatMap { version ->
            val effectiveFrom = LocalDate.parse(version["effectiveFrom"] as String)
            val effectiveTo = (version["effectiveTo"] as? String)?.let { LocalDate.parse(it) }
            val factors = version["factors"] as List<Map<String, Any>>
            factors.flatMap { factor ->
                val type = FactorType.valueOf(factor["type"] as String)
                val bands = factor["bands"] as List<Map<String, Any>>
                bands.map { band -> toRow(band, type, effectiveFrom, effectiveTo) }
            }
        }
    }

    private fun toRow(
        band: Map<String, Any>,
        type: FactorType,
        effectiveFrom: LocalDate,
        effectiveTo: LocalDate?,
    ) = RatingTable(
        factorType    = type,
        // YAML parses integers as Int and decimals as Double — BigDecimal via toString is safe for both
        bandStart     = BigDecimal(band["bandStart"].toString()),
        bandEnd       = BigDecimal(band["bandEnd"].toString()),
        factorValue   = BigDecimal(band["factorValue"].toString()),
        effectiveFrom = effectiveFrom,
        effectiveTo   = effectiveTo,
    )
}
