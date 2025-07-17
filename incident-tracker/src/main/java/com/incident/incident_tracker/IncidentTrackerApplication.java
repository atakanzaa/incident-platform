package com.incident.incident_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IncidentTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentTrackerApplication.class, args);
	}

}
