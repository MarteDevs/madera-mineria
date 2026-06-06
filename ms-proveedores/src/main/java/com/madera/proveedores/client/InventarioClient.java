package com.madera.proveedores.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ms-inventario",               // Eureka resuelve la URL
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {

    // Consultar si existe la madera
    @GetMapping("/api/inventario/{id}")
    Object obtenerMadera(@PathVariable("id") Long id);

    // Registrar entrada de stock (con responsable como 4to parametro corregido)
    @PostMapping("/api/inventario/{id}/entrada")
    void registrarEntrada(
        @PathVariable("id") Long id,
        @RequestParam("cantidad") Integer cantidad,
        @RequestParam("motivo") String motivo,
        @RequestParam("responsable") String responsable
    );

    // Consultar stock actual
    @GetMapping("/api/inventario/{id}/stock")
    Integer consultarStock(@PathVariable("id") Long id);
}
