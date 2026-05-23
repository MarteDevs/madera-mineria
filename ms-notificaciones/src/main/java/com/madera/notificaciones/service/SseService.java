package com.madera.notificaciones.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.madera.notificaciones.dto.NotificacionResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SseService {

    // Timeout de 15 minutos para la conexión (en milisegundos)
    private static final long TIMEOUT_MS = 15 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> emittersByRole = new ConcurrentHashMap<>();

    public SseEmitter agregarEmisor(String usuario, String rol) {
        log.info("Nueva solicitud de conexión SSE de usuario: {} con rol: {}", usuario, rol);
        
        // Si no hay rol, por defecto usamos ROLE_COMPRAS como fallback
        String rolNormalizado = rol != null ? rol.trim().toUpperCase() : "ROLE_COMPRAS";
        
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        
        emittersByRole.computeIfAbsent(rolNormalizado, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        emitter.onCompletion(() -> removerEmisor(rolNormalizado, emitter));
        emitter.onTimeout(() -> removerEmisor(rolNormalizado, emitter));
        emitter.onError((ex) -> removerEmisor(rolNormalizado, emitter));
        
        // Enviar evento inicial de conexión establecida
        try {
            emitter.send(SseEmitter.event()
                    .name("conectar")
                    .data("Conexión establecida correctamente"));
            log.debug("Evento de conexión enviado a {}", usuario);
        } catch (IOException e) {
            log.error("Error al enviar evento de conexión inicial a {}: {}", usuario, e.getMessage());
            emitter.completeWithError(e);
        }
        
        return emitter;
    }

    private void removerEmisor(String rol, SseEmitter emitter) {
        List<SseEmitter> list = emittersByRole.get(rol);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emittersByRole.remove(rol);
            }
        }
        log.debug("Conexión SSE removida para el rol: {}", rol);
    }

    public void enviarNotificacion(NotificacionResponse notificacion) {
        String destinatario = notificacion.getDestinatario() != null ? notificacion.getDestinatario().trim().toUpperCase() : "ADMIN";
        
        log.info("Propagando notificación id {} con destinatario {} vía SSE", notificacion.getId(), destinatario);
        
        // Determinar a qué roles enviar según el destinatario de la notificación
        List<String> rolesDestinatarios = obtenerRolesParaDestinatario(destinatario);
        
        for (String rol : rolesDestinatarios) {
            List<SseEmitter> activeEmitters = emittersByRole.get(rol);
            if (activeEmitters != null && !activeEmitters.isEmpty()) {
                List<SseEmitter> deadEmitters = new ArrayList<>();
                
                for (SseEmitter emitter : activeEmitters) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("notificacion")
                                .data(notificacion));
                        log.debug("Notificación enviada con éxito a un emisor del rol {}", rol);
                    } catch (IOException e) {
                        log.warn("Fallo al enviar notificación por SSE, marcando emisor para eliminación: {}", e.getMessage());
                        deadEmitters.add(emitter);
                    }
                }
                
                // Limpiar emisores fallidos
                for (SseEmitter dead : deadEmitters) {
                    removerEmisor(rol, dead);
                }
            }
        }
    }

    private List<String> obtenerRolesParaDestinatario(String destinatario) {
        List<String> roles = new ArrayList<>();
        // ROLE_ADMIN siempre recibe todas las notificaciones
        roles.add("ROLE_ADMIN");
        
        switch (destinatario) {
            case "ALMACEN":
                roles.add("ROLE_ALMACEN");
                break;
            case "COMPRAS":
                roles.add("ROLE_COMPRAS");
                break;
            case "TRANSPORTE":
                roles.add("ROLE_TRANSPORTE");
                break;
            default:
                break;
        }
        return roles;
    }

    // Heartbeat cada 25 segundos para evitar timeouts de routers/proxies
    @Scheduled(fixedDelay = 25000)
    public void enviarHeartbeat() {
        log.debug("Enviando SSE Heartbeat...");
        emittersByRole.forEach((rol, list) -> {
            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event()
                            .comment("ping"));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }
            
            for (SseEmitter dead : deadEmitters) {
                removerEmisor(rol, dead);
            }
        });
    }
}
