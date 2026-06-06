package com.madera.proveedores.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventarioClientFallback implements InventarioClient {

    @Override
    public Object obtenerMadera(Long id) {
        log.warn("ms-inventario no disponible. No se pudo obtener madera #{}", id);
        return null;
    }

    @Override
    public void registrarEntrada(Long id, Integer cantidad, String motivo, String responsable) {
        log.error("ms-inventario no disponible. " +
            "Stock NO actualizado para madera #{}: {} unidades (Responsable: {})", id, cantidad, responsable);
    }

    @Override
    public Integer consultarStock(Long id) {
        log.warn("ms-inventario no disponible. No se pudo consultar stock para madera #{}", id);
        return -1;
    }
}
