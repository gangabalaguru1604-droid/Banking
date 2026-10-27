package com.loanproject.loan_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class LoanAppApplication {

	public static void main(String[] args) {
		// Automatically load .env file into System properties if present
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> {
				if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
		} catch (Exception ignored) {
			// .env is optional (e.g. in cloud/Render where native env vars are used)
		}

		SpringApplication.run(LoanAppApplication.class, args);
	}

}
