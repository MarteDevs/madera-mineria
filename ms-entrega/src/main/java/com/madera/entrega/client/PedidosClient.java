package com.madera.entrega.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "ms-pedidos"
)
public interface PedidosClient {

    @PutMapping("/api/pedidos/{id}/estado")
    void actualizarEstadoPedido(@PathVariable("id") Long id,
                                @RequestParam("nuevoEstado") String nuevoEstado);
}
