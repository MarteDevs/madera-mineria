package com.madera.proveedores;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsProveedoresApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsProveedoresApplication.class, args);
	}

}
