package com.madera.mantenimiento.service;

import com.madera.mantenimiento.dto.*;

import java.util.List;

public interface MantenimientoService {

    VehiculoResponse registrarVehiculo(VehiculoRequest request);

    void procesarEntregaCompletada(EntregaCompletadaEvent evento);

    RegistroMantenimientoResponse registrarMantenimiento(Long vehiculoId, RegistroMantenimientoRequest request);

    List<VehiculoResponse> listarVehiculos();

    List<VehiculoResponse> vehiculosQueRequierenMantenimiento();

    List<AlertaMantenimientoResponse> alertasPendientes();

    List<RegistroMantenimientoResponse> historialVehiculo(Long id);

    VehiculoResponse obtenerPorPlaca(String placa);
}
