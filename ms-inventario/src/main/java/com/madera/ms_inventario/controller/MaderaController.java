package com.madera.ms_inventario.controller;

import com.madera.ms_inventario.dto.MaderaRequest;
import com.madera.ms_inventario.dto.MaderaResponse;
import com.madera.ms_inventario.model.MovimientoInventario;
import com.madera.ms_inventario.service.MaderaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
@RequiredArgsConstructor
public class MaderaController {

    private final MaderaService maderaService;

    @GetMapping
    public List<MaderaResponse> listarTodas() {
        return maderaService.listarTodas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaderaResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(maderaService.obtenerPorId(id));
    }

    @GetMapping("/mina/{mina}")
    public List<MaderaResponse> listarPorMina(@PathVariable String mina) {
        return maderaService.listarPorMina(mina);
    }

    @GetMapping("/tipo/{tipo}")
    public List<MaderaResponse> listarPorTipo(@PathVariable String tipo) {
        return maderaService.listarPorTipo(tipo);
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Integer> consultarStock(@PathVariable Long id) {
        return ResponseEntity.ok(maderaService.consultarStock(id));
    }

    @PostMapping
    public ResponseEntity<MaderaResponse> crear(@Valid @RequestBody MaderaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(maderaService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaderaResponse> actualizar(@PathVariable Long id, @Valid @RequestBody MaderaRequest request) {
        return ResponseEntity.ok(maderaService.actualizar(id, request));
    }

    @PutMapping("/{id}/reducir")
    public ResponseEntity<MaderaResponse> reducirStock(@PathVariable Long id, @RequestParam Integer cantidad) {
        return ResponseEntity.ok(maderaService.reducirStock(id, cantidad));
    }

    @GetMapping("/{id}/movimientos")
    public List<MovimientoInventario> listarMovimientos(@PathVariable Long id) {
        return maderaService.listarMovimientos(id);
    }

    @PostMapping("/{id}/entrada")
    public ResponseEntity<MaderaResponse> registrarEntrada(
            @PathVariable Long id,
            @RequestParam Integer cantidad,
            @RequestParam String motivo,
            @RequestParam String responsable) {
        return ResponseEntity.ok(maderaService.registrarEntrada(id, cantidad, motivo, responsable));
    }
}
