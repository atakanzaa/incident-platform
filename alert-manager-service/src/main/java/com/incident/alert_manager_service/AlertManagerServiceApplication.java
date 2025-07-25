package com.incident.alert_manager_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlertManagerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AlertManagerServiceApplication.class, args);
	}

}
