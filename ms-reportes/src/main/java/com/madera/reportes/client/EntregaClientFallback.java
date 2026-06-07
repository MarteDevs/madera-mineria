package com.madera.reportes.client;

import com.madera.reportes.dto.externos.EntregaDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class EntregaClientFallback implements EntregaClient {

    @Override
    public List<EntregaDto> listarTodas() {
        log.warn("ms-entrega no disponible. Usando fallback vacío en reportes.");
        return Collections.emptyList();
    }

    @Override
    public List<EntregaDto> enTransito() {
        log.warn("ms-entrega no disponible. Usando fallback vacío para entregas en tránsito.");
        return Collections.emptyList();
    }

    @Override
    public List<EntregaDto> porMina(String mina) {
        log.warn("ms-entrega no disponible para mina: {}. Usando fallback vacío.", mina);
        return Collections.emptyList();
    }
}
