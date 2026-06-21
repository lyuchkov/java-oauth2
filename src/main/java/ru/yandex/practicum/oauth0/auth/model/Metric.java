package ru.yandex.practicum.oauth0.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Metric {
    private Long id;
    private EventType eventType;
    private Long userId;
    private String clientId;
    private Instant eventTime;
    private String details;

    public enum EventType {
        ISSUE_SUCCESS, ISSUE_ERROR, REVOKE, VALIDATION_ERROR
    }
}