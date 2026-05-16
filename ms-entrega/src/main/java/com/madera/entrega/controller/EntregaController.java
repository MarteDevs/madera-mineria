package com.madera.entrega.controller;

import com.madera.entrega.dto.AsignarTransportistaRequest;
import com.madera.entrega.dto.ConfirmarRecepcionRequest;
import com.madera.entrega.dto.EntregaResponse;
import com.madera.entrega.model.EstadoEntrega;
import com.madera.entrega.service.EntregaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entregas")
@RequiredArgsConstructor
public class EntregaController {

    private final EntregaService entregaService;

    @GetMapping
    public ResponseEntity<List<EntregaResponse>> listarTodas() {
        return ResponseEntity.ok(entregaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntregaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(entregaService.obtenerPorId(id));
    }

    @GetMapping("/pedido/{pedidoId}")
    public ResponseEntity<EntregaResponse> obtenerPorPedido(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(entregaService.obtenerPorPedidoId(pedidoId));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<EntregaResponse>> porEstado(@PathVariable EstadoEntrega estado) {
        return ResponseEntity.ok(entregaService.listarPorEstado(estado));
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<EntregaResponse>> porMina(@PathVariable String mina) {
        return ResponseEntity.ok(entregaService.listarPorMina(mina));
    }

    @GetMapping("/en-transito")
    public ResponseEntity<List<EntregaResponse>> enTransito() {
        return ResponseEntity.ok(entregaService.listarEnTransito());
    }

    @PutMapping("/{id}/asignar-transportista")
    public ResponseEntity<EntregaResponse> asignarTransportista(
            @PathVariable Long id,
            @RequestBody @Valid AsignarTransportistaRequest request) {
        return ResponseEntity.ok(entregaService.asignarTransportista(id, request));
    }

    @PutMapping("/{id}/en-ruta")
    public ResponseEntity<EntregaResponse> marcarEnRuta(@PathVariable Long id) {
        return ResponseEntity.ok(entregaService.marcarEnRuta(id));
    }

    @PutMapping("/{id}/confirmar-recepcion")
    public ResponseEntity<EntregaResponse> confirmarRecepcion(
            @PathVariable Long id,
            @RequestBody @Valid ConfirmarRecepcionRequest request) {
        return ResponseEntity.ok(entregaService.confirmarRecepcion(id, request));
    }

    @PutMapping("/{id}/fallido")
    public ResponseEntity<EntregaResponse> marcarFallido(
            @PathVariable Long id,
            @RequestParam String motivo) {
        return ResponseEntity.ok(entregaService.marcarFallido(id, motivo));
    }
}
