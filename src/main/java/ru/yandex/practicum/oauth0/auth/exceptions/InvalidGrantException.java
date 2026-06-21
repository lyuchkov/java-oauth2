package ru.yandex.practicum.oauth0.auth.exceptions;

public class InvalidGrantException extends RuntimeException {
    public InvalidGrantException(String message) {
        super(message);
    }
}
