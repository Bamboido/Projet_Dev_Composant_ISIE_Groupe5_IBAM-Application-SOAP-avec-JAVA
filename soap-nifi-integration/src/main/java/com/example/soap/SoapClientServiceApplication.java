package com.example.soap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SoapClientServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SoapClientServiceApplication.class, args);
    }
}