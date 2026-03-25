package com.unibond;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnibondApplication {
    public static void main(String[] args) {
        SpringApplication.run(UnibondApplication.class, args);
    }
}
