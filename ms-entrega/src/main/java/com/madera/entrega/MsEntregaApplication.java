package com.madera.entrega;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsEntregaApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsEntregaApplication.class, args);
	}

}
