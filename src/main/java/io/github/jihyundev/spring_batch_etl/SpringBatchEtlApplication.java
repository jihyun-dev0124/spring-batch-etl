package io.github.jihyundev.spring_batch_etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
public class SpringBatchEtlApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchEtlApplication.class, args);
	}

}

