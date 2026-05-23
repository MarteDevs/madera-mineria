package com.madera.notificaciones.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.madera.notificaciones.dto.NotificacionResponse;
import com.madera.notificaciones.dto.PedidoEvent;
import com.madera.notificaciones.exception.ResourceNotFoundException;
import com.madera.notificaciones.model.EstadoNotificacion;
import com.madera.notificaciones.model.Notificacion;
import com.madera.notificaciones.repository.NotificacionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionServiceImpl implements NotificacionService {

    private static final String DESTINATARIO_ALMACEN = "ALMACEN";

    private final NotificacionRepository notificacionRepository;
    private final SseService sseService;

    @Override
    @Transactional
    public void procesarEvento(PedidoEvent evento) {
        log.info("Procesando evento {} para el pedido {}", evento.getEstado(), evento.getPedidoId());

        Notificacion notificacion = new Notificacion();
        notificacion.setPedidoId(evento.getPedidoId());
        notificacion.setTipoMadera(evento.getTipoMadera());
        notificacion.setCantidad(evento.getCantidad());
        notificacion.setMina(evento.getMina());
        notificacion.setAreaSolicitante(evento.getAreaSolicitante());
        notificacion.setTipoEvento(evento.getEstado());
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setMensaje(generarMensaje(evento));
        notificacion.setDestinatario(determinarDestinatario(evento.getEstado()));

        Notificacion guardada = notificacionRepository.save(notificacion);
        try {
            sseService.enviarNotificacion(mapToResponse(guardada));
        } catch (Exception e) {
            log.error("Error al enviar notificación por SSE: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public NotificacionResponse marcarLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada: " + id));

        notificacion.setEstado(EstadoNotificacion.LEIDA);
        notificacion.setFechaLeida(LocalDateTime.now());

        return mapToResponse(notificacionRepository.save(notificacion));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarTodas() {
        return notificacionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarPendientes() {
        return notificacionRepository.findByEstado(EstadoNotificacion.PENDIENTE).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarPorMina(String mina) {
        return notificacionRepository.findByMina(mina).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionResponse> listarPorDestinatario(String destinatario) {
        return notificacionRepository.findByDestinatario(destinatario).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientesAlmacen() {
        return notificacionRepository.countByDestinatarioAndEstado(
                DESTINATARIO_ALMACEN, EstadoNotificacion.PENDIENTE);
    }

    private String generarMensaje(PedidoEvent evento) {
        String estado = normalizarValor(evento.getEstado());

        return switch (estado) {
            case "CREADO" -> String.format(
                    "Nuevo pedido #%d: %d unidades de %s solicitadas por la mina %s. Requiere aprobación.",
                    evento.getPedidoId(),
                    evento.getCantidad(),
                    evento.getTipoMadera(),
                    evento.getMina());
            case "APROBADO" -> String.format(
                    "Pedido #%d aprobado. Preparar %d unidades de %s para despacho a %s.",
                    evento.getPedidoId(),
                    evento.getCantidad(),
                    evento.getTipoMadera(),
                    evento.getMina());
            case "RECHAZADO" -> String.format(
                    "Pedido #%d rechazado. Notificar a la mina %s.",
                    evento.getPedidoId(),
                    evento.getMina());
            case "PREPARANDO" -> String.format(
                    "Pedido #%d en preparación: transporte y chofer asignados para despacho a %s.",
                    evento.getPedidoId(),
                    evento.getMina());
            case "EN_RUTA" -> String.format(
                    "Pedido #%d en ruta: %d unidades de %s en tránsito hacia la mina %s.",
                    evento.getPedidoId(),
                    evento.getCantidad(),
                    evento.getTipoMadera(),
                    evento.getMina());
            case "ENTREGADO" -> String.format(
                    "Pedido #%d entregado: recepción conforme confirmada en la mina %s.",
                    evento.getPedidoId(),
                    evento.getMina());
            case "FALLIDO" -> String.format(
                    "Despacho fallido para el pedido #%d a la mina %s.",
                    evento.getPedidoId(),
                    evento.getMina());
            default -> String.format("Evento en pedido #%d: %s", evento.getPedidoId(), evento.getEstado());
        };
    }

    private String determinarDestinatario(String tipoEvento) {
        return switch (normalizarValor(tipoEvento)) {
            case "CREADO", "APROBADO", "PREPARANDO", "EN_RUTA", "ENTREGADO" -> DESTINATARIO_ALMACEN;
            case "RECHAZADO" -> "COMPRAS";
            default -> "ADMIN";
        };
    }

    private String normalizarValor(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private NotificacionResponse mapToResponse(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getPedidoId(),
                notificacion.getTipoMadera(),
                notificacion.getCantidad(),
                notificacion.getMina(),
                notificacion.getAreaSolicitante(),
                notificacion.getTipoEvento(),
                notificacion.getMensaje(),
                notificacion.getEstado().name(),
                notificacion.getDestinatario(),
                notificacion.getFechaRecibida(),
                notificacion.getFechaLeida());
    }
}
