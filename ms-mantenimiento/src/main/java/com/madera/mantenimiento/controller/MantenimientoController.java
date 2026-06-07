package com.madera.mantenimiento.controller;

import com.madera.mantenimiento.dto.*;
import com.madera.mantenimiento.service.MantenimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mantenimiento")
@RequiredArgsConstructor
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;

    // ── Vehículos ─────────────────────────────────────────────────────────────
    
    @GetMapping("/vehiculos")
    public ResponseEntity<List<VehiculoResponse>> listarVehiculos() {
        return ResponseEntity.ok(mantenimientoService.listarVehiculos());
    }

    @GetMapping("/vehiculos/{placa}")
    public ResponseEntity<VehiculoResponse> obtenerPorPlaca(@PathVariable String placa) {
        return ResponseEntity.ok(mantenimientoService.obtenerPorPlaca(placa));
    }

    @PostMapping("/vehiculos")
    public ResponseEntity<VehiculoResponse> registrarVehiculo(@RequestBody @Valid VehiculoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mantenimientoService.registrarVehiculo(request));
    }

    @GetMapping("/vehiculos/requieren-mantenimiento")
    public ResponseEntity<List<VehiculoResponse>> requierenMantenimiento() {
        return ResponseEntity.ok(mantenimientoService.vehiculosQueRequierenMantenimiento());
    }

    // ── Mantenimientos ────────────────────────────────────────────────────────
    
    @PostMapping("/vehiculos/{id}/mantenimiento")
    public ResponseEntity<RegistroMantenimientoResponse> registrarMantenimiento(
            @PathVariable Long id,
            @RequestBody @Valid RegistroMantenimientoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(mantenimientoService.registrarMantenimiento(id, request));
    }

    @GetMapping("/vehiculos/{id}/historial")
    public ResponseEntity<List<RegistroMantenimientoResponse>> historial(@PathVariable Long id) {
        return ResponseEntity.ok(mantenimientoService.historialVehiculo(id));
    }

    // ── Alertas ───────────────────────────────────────────────────────────────
    
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaMantenimientoResponse>> alertasPendientes() {
        return ResponseEntity.ok(mantenimientoService.alertasPendientes());
    }
}
