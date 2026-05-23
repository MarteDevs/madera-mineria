package com.madera.notificaciones.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.madera.notificaciones.dto.NotificacionResponse;
import com.madera.notificaciones.service.NotificacionService;
import com.madera.notificaciones.service.SseService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final SseService sseService;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotificaciones(
            @RequestHeader(value = "X-Usuario", required = false) String usuario,
            @RequestHeader(value = "X-Rol", required = false) String rol) {
        return sseService.agregarEmisor(usuario, rol);
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listarTodas() {
        return ResponseEntity.ok(notificacionService.listarTodas());
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<NotificacionResponse>> listarPendientes() {
        return ResponseEntity.ok(notificacionService.listarPendientes());
    }

    @GetMapping("/mina/{mina}")
    public ResponseEntity<List<NotificacionResponse>> listarPorMina(@PathVariable String mina) {
        return ResponseEntity.ok(notificacionService.listarPorMina(mina));
    }

    @GetMapping("/destinatario/{destinatario}")
    public ResponseEntity<List<NotificacionResponse>> listarPorDestinatario(
            @PathVariable String destinatario) {
        return ResponseEntity.ok(notificacionService.listarPorDestinatario(destinatario));
    }

    @GetMapping("/pendientes/count")
    public ResponseEntity<Long> contarPendientes() {
        return ResponseEntity.ok(notificacionService.contarPendientesAlmacen());
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }
}
