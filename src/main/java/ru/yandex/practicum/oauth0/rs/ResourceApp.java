package ru.yandex.practicum.oauth0.rs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

@ComponentScan
@SpringBootApplication
public class ResourceApp {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ResourceApp.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "8081"));
        app.run(args);
    }
}
