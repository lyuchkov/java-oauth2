package ru.yandex.practicum.oauth0.auth.exceptions;

public class InvalidClientException extends RuntimeException {
    public InvalidClientException(String message) {
        super(message);
    }
}
