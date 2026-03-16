package com.example.quoteEngine.quote.infrastructure

import com.example.quoteEngine.shared.events.DomainEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class QuoteEventPublisher(
    private val kafka: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val quoteEventRepository: QuoteEventRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(event: DomainEvent) {
        val key = event.aggregateId.toString()
        val payload = objectMapper.writeValueAsString(event)

        quoteEventRepository.save(
            QuoteEvent(
                quoteId = event.aggregateId,
                eventType = event.eventType,
                eventData = payload,
                occurredAt = event.occurredAt,
            ),
        )

        kafka
            .send(event.topic, key, payload)
            .whenComplete { _, ex ->
                if (ex != null) {
                    log.error(
                        "Failed to publish {} to topic={} key={}: {}",
                        event.eventType,
                        event.topic,
                        key,
                        ex.message,
                    )
                } else {
                    log.debug("Published {} to topic={} key={}", event.eventType, event.topic, key)
                }
            }
    }
}
