package com.example.quoteEngine.shared.api

import com.example.quoteEngine.quote.domain.InvalidTransitionException
import com.example.quoteEngine.quote.domain.QuoteNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(QuoteNotFoundException::class)
    fun handleNotFound(ex: QuoteNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(ex.message!!))

    @ExceptionHandler(InvalidTransitionException::class)
    fun handleInvalidTransition(ex: InvalidTransitionException): ResponseEntity<ErrorResponse> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ErrorResponse(ex.message!!))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val fieldErrors =
            ex.bindingResult.fieldErrors
                .map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(ErrorResponse("Validation failed", fieldErrors))
    }
}

data class ErrorResponse(
    val message: String,
    val fieldErrors: List<String> = emptyList(),
)
