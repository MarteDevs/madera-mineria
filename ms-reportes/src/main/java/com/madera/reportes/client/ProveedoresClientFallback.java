package com.madera.reportes.client;

import com.madera.reportes.dto.externos.ProveedorDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class ProveedoresClientFallback implements ProveedoresClient {

    @Override
    public List<ProveedorDto> listarTodos() {
        log.warn("ms-proveedores no disponible. Usando fallback vacío en reportes.");
        return Collections.emptyList();
    }

    @Override
    public List<ProveedorDto> listarActivos() {
        log.warn("ms-proveedores no disponible. Usando fallback vacío en reportes.");
        return Collections.emptyList();
    }

    @Override
    public List<Object> contratosProximosAVencer() {
        log.warn("ms-proveedores no disponible. No se pudo obtener contratos próximos a vencer.");
        return Collections.emptyList();
    }
}
