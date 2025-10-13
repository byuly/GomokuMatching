package com.gomokumatching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendApp {

	public static void main(String[] args) {
		SpringApplication.run(BackendApp.class, args);
	}

}
