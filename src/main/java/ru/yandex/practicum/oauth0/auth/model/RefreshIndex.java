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
public class RefreshIndex {
    private String refreshId;
    private Long userId;
    private String clientId;
    private Instant exp;
    private boolean rotated;
}