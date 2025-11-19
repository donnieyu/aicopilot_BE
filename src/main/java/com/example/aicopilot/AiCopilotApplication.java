package com.example.aicopilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class AiCopilotApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiCopilotApplication.class, args);
	}

}
