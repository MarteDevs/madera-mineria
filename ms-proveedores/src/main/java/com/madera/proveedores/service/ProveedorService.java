package com.madera.proveedores.service;

import com.madera.proveedores.dto.*;
import java.util.List;

public interface ProveedorService {
    ProveedorResponse crearProveedor(ProveedorRequest request);
    EntregaProveedorResponse registrarEntrega(Long proveedorId, EntregaProveedorRequest request);
    List<ProveedorResponse> listarTodos();
    List<ProveedorResponse> listarActivos();
    ProveedorResponse obtenerPorId(Long id);
    List<EntregaProveedorResponse> entregasPorProveedor(Long id);
    List<ContratoResponse> contratosPorProveedor(Long id);
    List<ContratoResponse> contratosProximosAVencer();
}
