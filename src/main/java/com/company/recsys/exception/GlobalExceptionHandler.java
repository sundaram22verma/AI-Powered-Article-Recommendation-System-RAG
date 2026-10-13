package com.company.recsys.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler for REST API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ArticleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleArticleNotFound(ArticleNotFoundException ex) {
        log.warn("Article not found: {}", ex.getArticleId());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "ARTICLE_NOT_FOUND", ex.getMessage(), ex.getArticleId());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error: ", ex);
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage()
                : "An unexpected error occurred. Please try again later.";
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                message, ex.getClass().getSimpleName());
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message, String detail) {
        ErrorResponse error = new ErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                detail
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Standard error response format.
     */
    public static class ErrorResponse {
        private final Instant timestamp;
        private final int status;
        private final String code;
        private final String message;
        private final String detail;

        public ErrorResponse(Instant timestamp, int status, String code, String message, String detail) {
            this.timestamp = timestamp;
            this.status = status;
            this.code = code;
            this.message = message;
            this.detail = detail;
        }

        public Instant getTimestamp() { return timestamp; }
        public int getStatus() { return status; }
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public String getDetail() { return detail; }
    }
}