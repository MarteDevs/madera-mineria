package com.madera.ms_inventario.service;

import com.madera.ms_inventario.dto.MaderaRequest;
import com.madera.ms_inventario.dto.MaderaResponse;
import com.madera.ms_inventario.model.MovimientoInventario;

import java.util.List;

public interface MaderaService {
    List<MaderaResponse> listarTodas();
    MaderaResponse obtenerPorId(Long id);
    List<MaderaResponse> listarPorMina(String mina);
    List<MaderaResponse> listarPorTipo(String tipo);
    Integer consultarStock(Long id);
    MaderaResponse crear(MaderaRequest request);
    MaderaResponse actualizar(Long id, MaderaRequest request);
    MaderaResponse reducirStock(Long id, Integer cantidad);
    List<MovimientoInventario> listarMovimientos(Long id);
    MaderaResponse registrarEntrada(Long id, Integer cantidad, String motivo, String responsable);
}
