package com.madera.reportes.client;

import com.madera.reportes.dto.externos.MaderaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(
    name = "ms-inventario",
    fallback = InventarioClientFallback.class
)
public interface InventarioClient {

    @GetMapping("/api/inventario")
    List<MaderaDto> listarTodo();

    @GetMapping("/api/inventario/mina/{mina}")
    List<MaderaDto> porMina(@PathVariable("mina") String mina);
}
