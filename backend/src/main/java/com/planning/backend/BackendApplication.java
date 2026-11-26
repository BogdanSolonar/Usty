package com.planning.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// Точка входу: запускає вбудований сервер Tomcat на порту 8080
@SpringBootApplication
@EnableAsync // для фонової відправки сповіщень у Telegram (NotificationService)
public class BackendApplication {

    public static void main(String[] args) {
        // Windows повідомляє Java стару назву поясу "Europe/Kiev", яку PostgreSQL
        // вже не приймає. Тому задаємо актуальну назву явно.
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        SpringApplication.run(BackendApplication.class, args);
    }
}
