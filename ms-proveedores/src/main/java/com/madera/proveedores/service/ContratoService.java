package com.madera.proveedores.service;

import com.madera.proveedores.dto.ContratoRequest;
import com.madera.proveedores.dto.ContratoResponse;
import java.util.List;

public interface ContratoService {
    ContratoResponse crearContrato(Long proveedorId, ContratoRequest request);
    List<ContratoResponse> listarTodosContratos();
    ContratoResponse actualizarEstadoContrato(Long contratoId, String nuevoEstado);
    ContratoResponse obtenerContratoPorId(Long id);
}
