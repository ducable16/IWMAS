package com.iwas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IWASApplication {

	public static void main(String[] args) {
		SpringApplication.run(IWASApplication.class, args);
	}

}
