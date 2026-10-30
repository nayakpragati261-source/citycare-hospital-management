package com.citycare.hospital.exception

import com.citycare.hospital.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.reason ?: "Request failed"
        return ResponseEntity.status(ex.statusCode).body(ErrorResponse(errorMessage))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errorMessage = ex.bindingResult.fieldErrors
            .firstOrNull()?.defaultMessage ?: "Invalid request parameters"
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(errorMessage))
    }

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception): ResponseEntity<ErrorResponse> {
        // Return simple JSON error response without exposing raw stack trace
        val errorResponse = ErrorResponse(
            message = ex.message ?: "Something went wrong"
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
