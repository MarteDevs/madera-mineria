package com.madera.reportes.client;

import com.madera.reportes.dto.externos.MaderaDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class InventarioClientFallback implements InventarioClient {

    @Override
    public List<MaderaDto> listarTodo() {
        log.warn("ms-inventario no disponible. Usando fallback vacío en reportes.");
        return Collections.emptyList();
    }

    @Override
    public List<MaderaDto> porMina(String mina) {
        log.warn("ms-inventario no disponible para la mina: {}. Usando fallback vacío.", mina);
        return Collections.emptyList();
    }
}
