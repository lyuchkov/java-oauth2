package ru.yandex.practicum.oauth0.auth.exceptions;

public class UnsupportedGrantTypeException extends RuntimeException {
    public UnsupportedGrantTypeException(String message) {
        super(message);
    }
}
