package com.madera.reportes.client;

import com.madera.reportes.dto.externos.PedidoDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class PedidosClientFallback implements PedidosClient {

    @Override
    public List<PedidoDto> listarTodos() {
        log.warn("ms-pedidos no disponible. Usando fallback vacío en reportes.");
        return Collections.emptyList();
    }

    @Override
    public List<PedidoDto> porMina(String mina) {
        log.warn("ms-pedidos no disponible para mina: {}. Usando fallback vacío.", mina);
        return Collections.emptyList();
    }

    @Override
    public List<PedidoDto> porEstado(String estado) {
        log.warn("ms-pedidos no disponible para estado: {}. Usando fallback vacío.", estado);
        return Collections.emptyList();
    }
}
