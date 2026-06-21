package ru.yandex.practicum.oauth0.auth.exceptions;

public class InsufficientScopeException extends RuntimeException {
    public InsufficientScopeException(String message) {
        super(message);
    }
}
