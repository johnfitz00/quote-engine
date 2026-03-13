package com.example.quoteEngine.quote.api

import com.example.quoteEngine.quote.api.dto.DriverResponse
import com.example.quoteEngine.quote.api.dto.QuoteResponse
import com.example.quoteEngine.quote.api.dto.VehicleResponse
import com.example.quoteEngine.quote.application.QuoteService
import com.example.quoteEngine.quote.domain.InvalidTransitionException
import com.example.quoteEngine.quote.domain.QuoteNotFoundException
import com.example.quoteEngine.quote.domain.QuoteStatus
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Instant
import java.util.UUID

@WebMvcTest(QuoteController::class)
class QuoteControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockkBean
    lateinit var quoteService: QuoteService

    companion object {
        private const val BASE_URL = "/v1/quotes"
    }

    // --- POST /v1/quotes ---

    @Nested
    inner class CreateQuote {

        @Test
        fun `returns 201 with Location header`() {
            val id = UUID.randomUUID()
            every { quoteService.createQuote(any()) } returns aQuoteResponse(id)

            mockMvc.post(BASE_URL) {
                contentType = MediaType.APPLICATION_JSON
                content = validCreateJson()
            }.andExpect {
                status { isCreated() }
                header { string("Location", "http://localhost$BASE_URL/$id") }
                jsonPath("$.id") { value(id.toString()) }
                jsonPath("$.status") { value("DRAFT") }
            }
        }

        @Test
        fun `invalid input returns 422 with field errors`() {
            mockMvc.post(BASE_URL) {
                contentType = MediaType.APPLICATION_JSON
                content = invalidCreateJson()
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.fieldErrors", hasItem<String>("policyHolderName: must not be blank"))
                jsonPath("$.fieldErrors", hasItem<String>("driver.age: must be greater than or equal to 16"))
            }
        }
    }

    // --- GET /v1/quotes/{id} ---

    @Nested
    inner class GetQuoteById {

        @Test
        fun `returns 200 with quote body`() {
            val id = UUID.randomUUID()
            every { quoteService.getQuote(id) } returns aQuoteResponse(id)

            mockMvc.get("$BASE_URL/$id").andExpect {
                status { isOk() }
                jsonPath("$.id") { value(id.toString()) }
                jsonPath("$.policyHolderName") { value("Alice Martin") }
            }
        }

        @Test
        fun `quote not found returns 404 with error body`() {
            val id = UUID.randomUUID()
            every { quoteService.getQuote(id) } throws QuoteNotFoundException(id)

            mockMvc.get("$BASE_URL/$id").andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Quote $id not found") }
            }
        }
    }

    // --- GET /v1/quotes ---

    @Nested
    inner class ListQuotes {

        @Test
        fun `returns 200 with all quotes when no filter`() {
            val quotes = listOf(aQuoteResponse(UUID.randomUUID()), aQuoteResponse(UUID.randomUUID()))
            every { quoteService.listQuotes(null) } returns quotes

            mockMvc.get(BASE_URL).andExpect {
                status { isOk() }
                jsonPath("$", hasSize<Any>(2))
            }
        }

        @Test
        fun `returns 200 filtered by status`() {
            val quote = aQuoteResponse(UUID.randomUUID(), status = "RATED")
            every { quoteService.listQuotes(QuoteStatus.RATED) } returns listOf(quote)

            mockMvc.get(BASE_URL) {
                param("status", "RATED")
            }.andExpect {
                status { isOk() }
                jsonPath("$", hasSize<Any>(1))
                jsonPath("$[0].status") { value("RATED") }
            }
        }
    }

    // --- PUT /v1/quotes/{id} ---

    @Nested
    inner class UpdateQuote {

        @Test
        fun `returns 200 with updated quote`() {
            val id = UUID.randomUUID()
            every { quoteService.updateQuote(id, any()) } returns aQuoteResponse(id, policyHolderName = "Bob Smith")

            mockMvc.put("$BASE_URL/$id") {
                contentType = MediaType.APPLICATION_JSON
                content = validUpdateJson("Bob Smith")
            }.andExpect {
                status { isOk() }
                jsonPath("$.policyHolderName") { value("Bob Smith") }
            }
        }

        @Test
        fun `invalid input returns 422 with field errors`() {
            val id = UUID.randomUUID()

            mockMvc.put("$BASE_URL/$id") {
                contentType = MediaType.APPLICATION_JSON
                content = invalidUpdateJson()
            }.andExpect {
                status { isUnprocessableContent() }
                jsonPath("$.message") { value("Validation failed") }
                jsonPath("$.fieldErrors", hasItem<String>("policyHolderName: must not be blank"))
            }
        }

        @Test
        fun `quote not found returns 404`() {
            val id = UUID.randomUUID()
            every { quoteService.updateQuote(id, any()) } throws QuoteNotFoundException(id)

            mockMvc.put("$BASE_URL/$id") {
                contentType = MediaType.APPLICATION_JSON
                content = validUpdateJson()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Quote $id not found") }
            }
        }

        @Test
        fun `non-DRAFT quote returns 409`() {
            val id = UUID.randomUUID()
            every { quoteService.updateQuote(id, any()) } throws
                InvalidTransitionException("Cannot edit a RATED quote — only DRAFT quotes can be updated")

            mockMvc.put("$BASE_URL/$id") {
                contentType = MediaType.APPLICATION_JSON
                content = validUpdateJson()
            }.andExpect {
                status { isConflict() }
                jsonPath("$.message") { value("Cannot edit a RATED quote — only DRAFT quotes can be updated") }
            }
        }
    }

    // --- POST /v1/quotes/{id}/rate ---

    @Nested
    inner class RateQuote {

        @Test
        fun `returns 202 with rated quote`() {
            val id = UUID.randomUUID()
            every { quoteService.rateQuote(id) } returns aQuoteResponse(id, status = "RATED")

            mockMvc.post("$BASE_URL/$id/rate").andExpect {
                status { isAccepted() }
                jsonPath("$.status") { value("RATED") }
            }
        }

        @Test
        fun `quote not found returns 404`() {
            val id = UUID.randomUUID()
            every { quoteService.rateQuote(id) } throws QuoteNotFoundException(id)

            mockMvc.post("$BASE_URL/$id/rate").andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Quote $id not found") }
            }
        }

        @Test
        fun `invalid transition returns 409`() {
            val id = UUID.randomUUID()
            every { quoteService.rateQuote(id) } throws
                InvalidTransitionException(QuoteStatus.BOUND, QuoteStatus.RATED)

            mockMvc.post("$BASE_URL/$id/rate").andExpect {
                status { isConflict() }
                jsonPath("$.message") { value("Cannot transition quote from BOUND to RATED") }
            }
        }
    }

    // --- POST /v1/quotes/{id}/bind ---

    @Nested
    inner class BindQuote {

        @Test
        fun `returns 200 with bound quote`() {
            val id = UUID.randomUUID()
            every { quoteService.bindQuote(id) } returns aQuoteResponse(id, status = "BOUND")

            mockMvc.post("$BASE_URL/$id/bind").andExpect {
                status { isOk() }
                jsonPath("$.status") { value("BOUND") }
            }
        }

        @Test
        fun `quote not found returns 404`() {
            val id = UUID.randomUUID()
            every { quoteService.bindQuote(id) } throws QuoteNotFoundException(id)

            mockMvc.post("$BASE_URL/$id/bind").andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Quote $id not found") }
            }
        }

        @Test
        fun `invalid transition returns 409`() {
            val id = UUID.randomUUID()
            every { quoteService.bindQuote(id) } throws
                InvalidTransitionException(QuoteStatus.DRAFT, QuoteStatus.BOUND)

            mockMvc.post("$BASE_URL/$id/bind").andExpect {
                status { isConflict() }
                jsonPath("$.message") { value("Cannot transition quote from DRAFT to BOUND") }
            }
        }
    }

    // --- DELETE /v1/quotes/{id} ---

    @Nested
    inner class DeclineQuote {

        @Test
        fun `returns 204 no content`() {
            val id = UUID.randomUUID()
            justRun { quoteService.decline(id) }

            mockMvc.delete("$BASE_URL/$id").andExpect {
                status { isNoContent() }
            }
        }

        @Test
        fun `quote not found returns 404`() {
            val id = UUID.randomUUID()
            every { quoteService.decline(id) } throws QuoteNotFoundException(id)

            mockMvc.delete("$BASE_URL/$id").andExpect {
                status { isNotFound() }
                jsonPath("$.message") { value("Quote $id not found") }
            }
        }

        @Test
        fun `invalid transition returns 409`() {
            val id = UUID.randomUUID()
            every { quoteService.decline(id) } throws
                InvalidTransitionException(QuoteStatus.BOUND, QuoteStatus.DECLINED)

            mockMvc.delete("$BASE_URL/$id").andExpect {
                status { isConflict() }
                jsonPath("$.message") { value("Cannot transition quote from BOUND to DECLINED") }
            }
        }
    }

    // --- Fixtures ---

    private fun aQuoteResponse(
        id: UUID = UUID.randomUUID(),
        policyHolderName: String = "Alice Martin",
        status: String = "DRAFT"
    ) = QuoteResponse(
        id = id,
        policyHolderName = policyHolderName,
        vehicle = VehicleResponse(year = 2020, make = "Toyota", model = "Corolla", annualKm = 15000),
        driver = DriverResponse(age = 34, licenceYears = 12, atFaultClaims = 0),
        status = status,
        createdAt = Instant.now(),
        updatedAt = Instant.now()
    )

    private fun validCreateJson() = """
        {
          "policyHolderName": "Alice Martin",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 34, "licenceYears": 12, "atFaultClaims": 0 }
        }
    """.trimIndent()

    private fun invalidCreateJson() = """
        {
          "policyHolderName": "",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 14, "licenceYears": 0, "atFaultClaims": 0 }
        }
    """.trimIndent()

    private fun validUpdateJson(name: String = "Alice Martin") = """
        {
          "policyHolderName": "$name",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 34, "licenceYears": 12, "atFaultClaims": 0 }
        }
    """.trimIndent()

    private fun invalidUpdateJson() = """
        {
          "policyHolderName": "",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 34, "licenceYears": 12, "atFaultClaims": 0 }
        }
    """.trimIndent()
}
