package com.madera.pedidos.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<Map<String, String>> handleStock(StockInsuficienteException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Stock insuficiente",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(PedidoNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(PedidoNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "Pedido no encontrado",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleEstado(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Cambio de estado inválido",
            "mensaje", ex.getMessage()
        ));
    }
}
