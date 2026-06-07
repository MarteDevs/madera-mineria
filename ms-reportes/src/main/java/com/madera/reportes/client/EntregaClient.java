package com.madera.reportes.client;

import com.madera.reportes.dto.externos.EntregaDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(
    name = "ms-entrega",
    fallback = EntregaClientFallback.class
)
public interface EntregaClient {

    @GetMapping("/api/entregas")
    List<EntregaDto> listarTodas();

    @GetMapping("/api/entregas/en-transito")
    List<EntregaDto> enTransito();

    @GetMapping("/api/entregas/mina/{mina}")
    List<EntregaDto> porMina(@PathVariable("mina") String mina);
}
