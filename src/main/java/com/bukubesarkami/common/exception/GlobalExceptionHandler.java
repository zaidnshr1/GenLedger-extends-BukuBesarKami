package com.bukubesarkami.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    record ErrorResponse(int status, String error, Object message, OffsetDateTime timestamp) {}

    @ExceptionHandler(AppException.NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(AppException.NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(AppException.DuplicateException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(AppException.DuplicateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({AppException.AccessDeniedException.class,
            org.springframework.security.access.AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(RuntimeException ex) {
        return build(HttpStatus.FORBIDDEN, "Akses ditolak.");
    }

    @ExceptionHandler({AppException.BusinessException.class,
            AppException.UnbalancedEntryException.class})
    public ResponseEntity<ErrorResponse> handleBusiness(RuntimeException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(AppException.TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(AppException.TooManyRequestsException ex) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler({AppException.InvalidTokenException.class,
            BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Username atau password salah.");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(DisabledException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Akun tidak aktif.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return build(HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Terjadi kesalahan sistem.");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, Object message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(status.value(), status.getReasonPhrase(), message, OffsetDateTime.now())
        );
    }
}