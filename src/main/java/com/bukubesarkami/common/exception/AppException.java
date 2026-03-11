package com.bukubesarkami.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public final class AppException {

    private AppException() {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class DuplicateException extends RuntimeException {
        public DuplicateException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class AccessDeniedException extends RuntimeException {
        public AccessDeniedException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public static class UnbalancedEntryException extends RuntimeException {
        public UnbalancedEntryException() {
            super("Jurnal tidak seimbang: total debit harus sama dengan total kredit.");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public static class InvalidTokenException extends RuntimeException {
        public InvalidTokenException(String message) { super(message); }
    }

    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public static class TooManyRequestsException extends RuntimeException {
        public TooManyRequestsException(String message) { super(message); }
    }
}