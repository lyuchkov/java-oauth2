package ru.yandex.practicum.oauth0.rs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Getter
@Component
public class RsProperties {

    private final ResourceLoader resourceLoader;

    private String secret;
    private String aud;

    public RsProperties(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        Resource resource = resourceLoader.getResource("classpath:config/rs.json");

        if (!resource.exists()) {
            throw new IllegalStateException("Configuration file config/rs.json not found in classpath!");
        }

        try (InputStream inputStream = resource.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode config = mapper.readTree(inputStream);

            this.secret = config.get("secret").asText();
            this.aud = config.get("aud").asText();
        }
    }
}