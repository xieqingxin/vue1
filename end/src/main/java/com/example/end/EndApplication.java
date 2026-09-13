package com.example.end;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EndApplication {

    public static void main(String[] args) {
        SpringApplication.run(EndApplication.class, args);
    }
}
