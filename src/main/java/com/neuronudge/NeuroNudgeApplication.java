package com.neuronudge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class NeuroNudgeApplication {
    public static void main(String[] args) {
        SpringApplication.run(NeuroNudgeApplication.class, args);
    }
}
    