package com.PaperPilot.exception;

import com.PaperPilot.dto.ApiFromResponse;
import org.slf4j.Logger;
import org.springdoc.api.OpenApiResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please try again later.";

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleDocumentProcessingException(DocumentProcessingException ex) {
        logger.warn("Document processing error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        logger.error("File size exceeded: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message("File size exceeds the maximum limit.")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        logger.error("Validation error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message("Validation failed for the request.")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OpenApiResourceNotFoundException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleOpenApiResourceNotFoundException(OpenApiResourceNotFoundException ex) {
        logger.error("OpenAPI resource not found: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message("Requested OpenAPI resource not found.")
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleAiServiceException(AiServiceException ex) {
        HttpStatus status = ex.isTimeout() ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.BAD_GATEWAY;
        logger.error("AI provider error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiFromResponse<Object>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message(GENERIC_ERROR_MESSAGE)
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiFromResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(ApiFromResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .timestamp(java.time.LocalDateTime.now())
                .build(), HttpStatus.NOT_FOUND);
    }
}
