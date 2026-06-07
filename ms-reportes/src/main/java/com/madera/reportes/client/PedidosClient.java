package com.madera.reportes.client;

import com.madera.reportes.dto.externos.PedidoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(
    name = "ms-pedidos",
    fallback = PedidosClientFallback.class
)
public interface PedidosClient {

    @GetMapping("/api/pedidos")
    List<PedidoDto> listarTodos();

    @GetMapping("/api/pedidos/mina/{mina}")
    List<PedidoDto> porMina(@PathVariable("mina") String mina);

    @GetMapping("/api/pedidos/estado/{estado}")
    List<PedidoDto> porEstado(@PathVariable("estado") String estado);
}
