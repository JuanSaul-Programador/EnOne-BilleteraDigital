package com.enone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@SpringBootApplication
@EnableTransactionManagement
public class EnOneSpringBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnOneSpringBootApplication.class, args);
	}

}
