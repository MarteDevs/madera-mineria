package com.madera.proveedores.controller;

import com.madera.proveedores.dto.ContratoRequest;
import com.madera.proveedores.dto.ContratoResponse;
import com.madera.proveedores.service.ContratoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoService contratoService;

    @PostMapping("/proveedor/{proveedorId}")
    public ResponseEntity<ContratoResponse> crear(
            @PathVariable Long proveedorId,
            @RequestBody @Valid ContratoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(contratoService.crearContrato(proveedorId, request));
    }

    @GetMapping
    public ResponseEntity<List<ContratoResponse>> listarTodos() {
        return ResponseEntity.ok(contratoService.listarTodosContratos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContratoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(contratoService.obtenerContratoPorId(id));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<ContratoResponse> actualizarEstado(
            @PathVariable Long id,
            @RequestParam String estado) {
        return ResponseEntity.ok(contratoService.actualizarEstadoContrato(id, estado));
    }
}
