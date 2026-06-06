package com.madera.proveedores.controller;

import com.madera.proveedores.dto.*;
import com.madera.proveedores.service.ProveedorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public ResponseEntity<List<ProveedorResponse>> listarTodos() {
        return ResponseEntity.ok(proveedorService.listarTodos());
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ProveedorResponse>> listarActivos() {
        return ResponseEntity.ok(proveedorService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProveedorResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(
            @RequestBody @Valid ProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(proveedorService.crearProveedor(request));
    }

    @GetMapping("/{id}/entregas")
    public ResponseEntity<List<EntregaProveedorResponse>> entregas(
            @PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.entregasPorProveedor(id));
    }

    @PostMapping("/{id}/entregas")
    public ResponseEntity<EntregaProveedorResponse> registrarEntrega(
            @PathVariable Long id,
            @RequestBody @Valid EntregaProveedorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(proveedorService.registrarEntrega(id, request));
    }

    @GetMapping("/{id}/contratos")
    public ResponseEntity<List<ContratoResponse>> contratos(
            @PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.contratosPorProveedor(id));
    }

    @GetMapping("/contratos/proximos-a-vencer")
    public ResponseEntity<List<ContratoResponse>> proximosAVencer() {
        return ResponseEntity.ok(proveedorService.contratosProximosAVencer());
    }
}
