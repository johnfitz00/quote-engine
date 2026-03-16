# Quote Engine

[![CI](https://github.com/johnfitz00/quote-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/johnfitz00/quote-engine/actions/workflows/ci.yml)

A motor insurance quoting service built with Spring Boot and Kotlin. Handles the full quote lifecycle from initial submission through rating, referral, and binding, using an event-driven architecture via Kafka.

## Quote lifecycle

```
DRAFT → RATING_IN_PROGRESS → RATED → BOUND
                                    → REFERRED → BOUND
                                               → DECLINED
                                    → DECLINED
                                    → EXPIRED
      → DECLINED
```

When a quote is submitted for rating the service publishes a `QuoteRatingRequested` event to Kafka. The rating consumer picks this up, calculates a premium using a versioned factor-based engine, and updates the quote to `RATED`.

## Rating engine

Premiums are calculated by multiplying a base rate against a set of underwriting factors loaded from `motor-rates.yml`. Two rate versions are shipped (2024 and 2025), and the engine selects the version effective on the quote's inception date. Factors cover:

| Factor | Description |
|---|---|
| AGE | Driver age band loading |
| VEHICLE_AGE | Vehicle age band loading |
| USAGE | Annual kilometres driven |
| CLAIMS | At-fault claims in the past 3 years |
| NCB | No-claims bonus years |

## Tech stack

- **Kotlin 2.2 / Java 21**
- **Spring Boot 4** — web, JPA, validation
- **Apache Kafka** — async quote rating pipeline
- **H2** — in-memory database (dev/test)
- **detekt + ktlint** — static analysis and code style

## Running locally

Start Kafka (Confluent Platform via Docker Compose):

```bash
docker compose up -d
```

Then run the application:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

The API is available at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/v1/quotes` | Create a new quote in `DRAFT` |
| `GET` | `/v1/quotes` | List quotes (optional `?status=` filter) |
| `GET` | `/v1/quotes/{id}` | Get a single quote |
| `PUT` | `/v1/quotes/{id}` | Update a `DRAFT` quote |
| `POST` | `/v1/quotes/{id}/rate` | Submit for rating → `RATING_IN_PROGRESS` |
| `POST` | `/v1/quotes/{id}/bind` | Bind a `RATED` or `REFERRED` quote |
| `DELETE` | `/v1/quotes/{id}` | Decline a quote |

## Tests

```bash
./gradlew test          # unit + integration tests
./gradlew detekt        # static analysis
./gradlew ktlintCheck   # code style
```

Integration tests use an embedded Kafka broker — no external services required.
