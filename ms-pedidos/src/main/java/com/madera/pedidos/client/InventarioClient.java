package com.madera.pedidos.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ms-inventario",
    url = "${feign.client.config.ms-inventario.url:http://localhost:8082}"
)
public interface InventarioClient {

    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable("id") Long id);

    @PutMapping("/api/inventario/{id}/reducir")
    void reducirStock(@PathVariable("id") Long id,
                      @RequestParam("cantidad") Integer cantidad);
}
