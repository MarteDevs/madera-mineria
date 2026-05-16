package com.madera.entrega.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntregaNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(EntregaNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "Entrega no encontrada",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleEstado(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", "Cambio de estado invalido",
            "mensaje", ex.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest().body(Map.of(
            "error", "Datos invalidos",
            "mensaje", mensaje
        ));
    }
}
