package ru.yandex.practicum.oauth0.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {
    private Long roleId;
    private String name;
    private Set<String> scopes;
}