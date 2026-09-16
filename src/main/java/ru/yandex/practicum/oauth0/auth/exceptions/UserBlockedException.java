package ru.yandex.practicum.oauth0.auth.exceptions;

public class UserBlockedException extends RuntimeException {
    public UserBlockedException(String message) {
        super(message);
    }
}
