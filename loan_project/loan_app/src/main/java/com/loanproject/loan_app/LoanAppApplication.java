package com.loanproject.loan_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LoanAppApplication {

	public static void main(String[] args) {
		
		SpringApplication.run(LoanAppApplication.class, args);
	
	}

}
