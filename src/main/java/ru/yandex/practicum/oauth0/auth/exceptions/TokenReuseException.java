package ru.yandex.practicum.oauth0.auth.exceptions;

public class TokenReuseException extends RuntimeException {
    public TokenReuseException(String message) {
        super(message);
    }
}
