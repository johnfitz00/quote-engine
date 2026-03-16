package com.example.quoteEngine.quote.api

import com.example.quoteEngine.quote.infrastructure.QuoteRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
class QuoteIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var quoteRepository: QuoteRepository

    companion object {
        private const val BASE_URL = "/v1/quotes"
    }

    @BeforeEach
    fun setUp() {
        quoteRepository.deleteAll()
    }

    // --- Core lifecycle ---

    @Test
    fun `full lifecycle - create, retrieve, rate, confirm status`() {
        // 1. POST creates a quote in DRAFT status
        val location = mockMvc.post(BASE_URL) {
            contentType = MediaType.APPLICATION_JSON
            content = validCreateJson()
        }.andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("DRAFT") }
            jsonPath("$.policyHolderName") { value("Alice Martin") }
        }.andReturn().response.getHeader("Location")!!

        val id = location.substringAfterLast("/")

        // 2. GET retrieves the persisted quote
        mockMvc.get("$BASE_URL/$id").andExpect {
            status { isOk() }
            jsonPath("$.id") { value(id) }
            jsonPath("$.status") { value("DRAFT") }
            jsonPath("$.policyHolderName") { value("Alice Martin") }
            jsonPath("$.vehicle.make") { value("Toyota") }
        }

        // 3. POST /rate transitions to RATED
        mockMvc.post("$BASE_URL/$id/rate").andExpect {
            status { isAccepted() }
            jsonPath("$.status") { value("RATED") }
        }

        // 4. GET confirms the persisted status change
        mockMvc.get("$BASE_URL/$id").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("RATED") }
        }
    }

    @Test
    fun `full bind flow - create, rate, bind`() {
        val id = createQuoteAndExtractId()

        mockMvc.post("$BASE_URL/$id/rate").andExpect {
            status { isAccepted() }
            jsonPath("$.status") { value("RATED") }
        }

        mockMvc.post("$BASE_URL/$id/bind").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("BOUND") }
        }
    }

    // --- List ---

    @Test
    fun `list returns all created quotes`() {
        repeat(2) { createQuoteAndExtractId() }

        mockMvc.get(BASE_URL).andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun `list filtered by status returns only matching quotes`() {
        val id = createQuoteAndExtractId()
        mockMvc.post("$BASE_URL/$id/rate")   // DRAFT → RATED
        createQuoteAndExtractId()            // second stays DRAFT

        mockMvc.get(BASE_URL) { param("status", "RATED") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].status") { value("RATED") }
        }

        mockMvc.get(BASE_URL) { param("status", "DRAFT") }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }
    }

    // --- Update ---

    @Test
    fun `update persists changes to a DRAFT quote`() {
        val id = createQuoteAndExtractId()

        mockMvc.put("$BASE_URL/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = validUpdateJson("Bob Smith")
        }.andExpect {
            status { isOk() }
            jsonPath("$.policyHolderName") { value("Bob Smith") }
        }

        mockMvc.get("$BASE_URL/$id").andExpect {
            status { isOk() }
            jsonPath("$.policyHolderName") { value("Bob Smith") }
        }
    }

    // --- Decline ---

    @Test
    fun `decline transitions quote to DECLINED`() {
        val id = createQuoteAndExtractId()

        mockMvc.delete("$BASE_URL/$id").andExpect {
            status { isNoContent() }
        }

        mockMvc.get("$BASE_URL/$id").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DECLINED") }
        }
    }

    // --- Error paths ---

    @Test
    fun `get non-existent quote returns 404`() {
        mockMvc.get("$BASE_URL/00000000-0000-0000-0000-000000000000").andExpect {
            status { isNotFound() }
            jsonPath("$.message") { value("Quote 00000000-0000-0000-0000-000000000000 not found") }
        }
    }

    @Test
    fun `invalid transition returns 409 with error message`() {
        val id = createQuoteAndExtractId()

        // DRAFT → BOUND is not an allowed transition
        mockMvc.post("$BASE_URL/$id/bind").andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Cannot transition quote from DRAFT to BOUND") }
        }
    }

    @Test
    fun `update non-DRAFT quote returns 409`() {
        val id = createQuoteAndExtractId()
        mockMvc.post("$BASE_URL/$id/rate")  // DRAFT → RATED

        mockMvc.put("$BASE_URL/$id") {
            contentType = MediaType.APPLICATION_JSON
            content = validUpdateJson("Bob Smith")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("Cannot edit a RATED quote — only DRAFT quotes can be updated") }
        }
    }

    // --- Helpers ---

    private fun createQuoteAndExtractId(): String {
        val location = mockMvc.post(BASE_URL) {
            contentType = MediaType.APPLICATION_JSON
            content = validCreateJson()
        }.andReturn().response.getHeader("Location")!!
        return location.substringAfterLast("/")
    }

    private fun validCreateJson() = """
        {
          "policyHolderName": "Alice Martin",
          "state": "NSW",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 34, "licenceYears": 12, "atFaultClaims": 0 }
        }
    """.trimIndent()

    private fun validUpdateJson(name: String) = """
        {
          "policyHolderName": "$name",
          "state": "NSW",
          "vehicle": { "year": 2020, "make": "Toyota", "model": "Corolla", "annualKm": 15000 },
          "driver": { "age": 34, "licenceYears": 12, "atFaultClaims": 0 }
        }
    """.trimIndent()
}
