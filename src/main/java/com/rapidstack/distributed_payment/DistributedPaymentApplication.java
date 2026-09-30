package com.rapidstack.distributed_payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DistributedPaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributedPaymentApplication.class, args);
	}

}
