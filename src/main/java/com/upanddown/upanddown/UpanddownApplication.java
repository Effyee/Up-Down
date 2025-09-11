package com.upanddown.upanddown;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UpanddownApplication {

	public static void main(String[] args) {
		SpringApplication.run(UpanddownApplication.class, args);
	}

}
