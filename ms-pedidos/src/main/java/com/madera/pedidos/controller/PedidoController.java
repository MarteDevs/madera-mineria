package com.madera.pedidos.controller;

import com.madera.pedidos.dto.PedidoRequest;
import com.madera.pedidos.dto.PedidoResponse;
import com.madera.pedidos.model.EstadoPedido;
import com.madera.pedidos.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.obtenerPorId(id));
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<PedidoResponse>> porMina(@PathVariable String mina) {
        return ResponseEntity.ok(pedidoService.listarPorMina(mina));
    }

    @GetMapping("/estado/{estado}")
    public ResponseEntity<List<PedidoResponse>> porEstado(
            @PathVariable EstadoPedido estado) {
        return ResponseEntity.ok(pedidoService.listarPorEstado(estado));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(201).body(pedidoService.crearPedido(request));
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<PedidoResponse> aprobar(
            @PathVariable Long id,
            @RequestParam String aprobadoPor) {
        return ResponseEntity.ok(pedidoService.aprobarPedido(id, aprobadoPor));
    }

    @PutMapping("/{id}/rechazar")
    public ResponseEntity<PedidoResponse> rechazar(
            @PathVariable Long id,
            @RequestParam String motivo) {
        return ResponseEntity.ok(pedidoService.rechazarPedido(id, motivo));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponse> cambiarEstado(
            @PathVariable Long id,
            @RequestParam EstadoPedido nuevoEstado) {
        return ResponseEntity.ok(pedidoService.cambiarEstado(id, nuevoEstado));
    }
}
