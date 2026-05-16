package com.madera.entrega.service;

import com.madera.entrega.client.PedidosClient;
import com.madera.entrega.dto.AsignarTransportistaRequest;
import com.madera.entrega.dto.ConfirmarRecepcionRequest;
import com.madera.entrega.dto.EntregaResponse;
import com.madera.entrega.dto.PedidoAprobadoEvent;
import com.madera.entrega.exception.EntregaNotFoundException;
import com.madera.entrega.model.Entrega;
import com.madera.entrega.model.EstadoEntrega;
import com.madera.entrega.model.HistorialEntrega;
import com.madera.entrega.repository.EntregaRepository;
import com.madera.entrega.repository.HistorialEntregaRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EntregaServiceImpl implements EntregaService {

    private final EntregaRepository entregaRepository;
    private final HistorialEntregaRepository historialEntregaRepository;
    private final PedidosClient pedidosClient;

    @Override
    @Transactional
    public void crearEntregaDesdeEvento(PedidoAprobadoEvent evento) {
        if (entregaRepository.findByPedidoId(evento.getPedidoId()).isPresent()) {
            log.warn("Ya existe una entrega para el pedido #{}", evento.getPedidoId());
            return;
        }

        Entrega entrega = Entrega.builder()
            .pedidoId(evento.getPedidoId())
            .tipoMadera(evento.getTipoMadera())
            .cantidad(evento.getCantidad())
            .unidad(evento.getUnidad())
            .almacenOrigen("Almacen Central")
            .minaDestino(evento.getMina())
            .estado(EstadoEntrega.PENDIENTE)
            .fechaEntregaEstimada(LocalDateTime.now().plusDays(2))
            .observaciones("Entrega creada automaticamente desde pedido aprobado")
            .build();

        entrega = entregaRepository.save(entrega);
        registrarHistorial(entrega, null, EstadoEntrega.PENDIENTE, "RabbitMQ", "Creacion automatica desde pedido aprobado");
    }

    @Override
    @Transactional
    public EntregaResponse asignarTransportista(Long id, AsignarTransportistaRequest request) {
        Entrega entrega = obtenerEntidadPorId(id);

        if (entrega.getEstado() != EstadoEntrega.PENDIENTE) {
            throw new IllegalStateException("Solo se puede asignar transportista a entregas PENDIENTES");
        }

        EstadoEntrega estadoAnterior = entrega.getEstado();

        entrega.setTransportista(request.getTransportista());
        entrega.setVehiculo(request.getVehiculo());
        entrega.setTelefTransportista(request.getTelefTransportista());
        entrega.setAlmacenOrigen(request.getAlmacenOrigen());
        entrega.setDireccionMina(request.getDireccionMina());
        entrega.setDistanciaKm(request.getDistanciaKm());
        entrega.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        entrega.setEstado(EstadoEntrega.PREPARANDO);

        entrega = entregaRepository.save(entrega);
        registrarHistorial(entrega, estadoAnterior, EstadoEntrega.PREPARANDO, request.getTransportista(), "Transportista y vehiculo asignados");
        return mapToResponse(entrega);
    }

    @Override
    @Transactional
    public EntregaResponse marcarEnRuta(Long id) {
        Entrega entrega = obtenerEntidadPorId(id);

        if (entrega.getEstado() != EstadoEntrega.PREPARANDO) {
            throw new IllegalStateException("La entrega debe estar en estado PREPARANDO para salir");
        }

        EstadoEntrega estadoAnterior = entrega.getEstado();

        entrega.setEstado(EstadoEntrega.EN_RUTA);
        entrega.setFechaSalida(LocalDateTime.now());

        entrega = entregaRepository.save(entrega);
        registrarHistorial(entrega, estadoAnterior, EstadoEntrega.EN_RUTA, entrega.getTransportista(), "Salida del almacen hacia la mina");
        return mapToResponse(entrega);
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "pedidos", fallbackMethod = "fallbackConfirmarRecepcion")
    @Retry(name = "pedidos")
    public EntregaResponse confirmarRecepcion(Long id, ConfirmarRecepcionRequest request) {
        Entrega entrega = obtenerEntidadPorId(id);

        if (entrega.getEstado() != EstadoEntrega.EN_RUTA) {
            throw new IllegalStateException("Solo se puede confirmar recepcion de entregas EN_RUTA");
        }

        EstadoEntrega estadoAnterior = entrega.getEstado();

        entrega.setEstado(EstadoEntrega.ENTREGADO);
        entrega.setFechaEntregaReal(LocalDateTime.now());
        entrega.setRecibidoPor(request.getRecibidoPor());
        entrega.setConformidad(request.getConformidad());
        entrega.setObservaciones(request.getObservaciones());

        entrega = entregaRepository.save(entrega);
        registrarHistorial(entrega, estadoAnterior, EstadoEntrega.ENTREGADO, request.getRecibidoPor(), "Recepcion confirmada en mina");

        pedidosClient.actualizarEstadoPedido(entrega.getPedidoId(), "ENTREGADO");
        return mapToResponse(entrega);
    }

    // ─── FALLBACK para confirmar recepcion ──────────────────────────────────
    public EntregaResponse fallbackConfirmarRecepcion(Long id, ConfirmarRecepcionRequest request, Exception ex) {
        log.error("No se pudo actualizar estado del pedido #{} en ms-pedidos. Circuit Breaker activo.", id);
        
        Entrega entrega = obtenerEntidadPorId(id);
        // Retornamos la entrega pero con un mensaje de advertencia
        EntregaResponse response = mapToResponse(entrega);
        response.setObservaciones("AVISO: Entrega registrada localmente pero no se pudo notificar a ms-pedidos. Motivo: " + ex.getMessage());
        return response;
    }

    @Override
    @Transactional
    public EntregaResponse marcarFallido(Long id, String motivo) {
        Entrega entrega = obtenerEntidadPorId(id);
        EstadoEntrega estadoAnterior = entrega.getEstado();

        entrega.setEstado(EstadoEntrega.FALLIDO);
        entrega.setObservaciones(motivo);

        entrega = entregaRepository.save(entrega);
        registrarHistorial(entrega, estadoAnterior, EstadoEntrega.FALLIDO, "admin", motivo);
        return mapToResponse(entrega);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listarTodas() {
        return entregaRepository.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EntregaResponse obtenerPorId(Long id) {
        return mapToResponse(obtenerEntidadPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public EntregaResponse obtenerPorPedidoId(Long pedidoId) {
        Entrega entrega = entregaRepository.findByPedidoId(pedidoId)
            .orElseThrow(() -> new EntregaNotFoundException("No existe entrega para el pedido: " + pedidoId));
        return mapToResponse(entrega);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listarPorEstado(EstadoEntrega estado) {
        return entregaRepository.findByEstado(estado).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listarPorMina(String mina) {
        return entregaRepository.findByMinaDestino(mina).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntregaResponse> listarEnTransito() {
        return entregaRepository.findByEstadoIn(List.of(EstadoEntrega.PREPARANDO, EstadoEntrega.EN_RUTA))
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    private Entrega obtenerEntidadPorId(Long id) {
        return entregaRepository.findById(id)
            .orElseThrow(() -> new EntregaNotFoundException("Entrega no encontrada: " + id));
    }

    private void registrarHistorial(Entrega entrega,
                                    EstadoEntrega estadoAnterior,
                                    EstadoEntrega estadoNuevo,
                                    String responsable,
                                    String motivo) {
        historialEntregaRepository.save(HistorialEntrega.builder()
            .entrega(entrega)
            .estadoAnterior(estadoAnterior)
            .estadoNuevo(estadoNuevo)
            .responsable(responsable)
            .motivo(motivo)
            .build());
    }

    private EntregaResponse mapToResponse(Entrega entrega) {
        return EntregaResponse.builder()
            .id(entrega.getId())
            .pedidoId(entrega.getPedidoId())
            .tipoMadera(entrega.getTipoMadera())
            .cantidad(entrega.getCantidad())
            .unidad(entrega.getUnidad())
            .almacenOrigen(entrega.getAlmacenOrigen())
            .minaDestino(entrega.getMinaDestino())
            .direccionMina(entrega.getDireccionMina())
            .distanciaKm(entrega.getDistanciaKm())
            .transportista(entrega.getTransportista())
            .vehiculo(entrega.getVehiculo())
            .telefTransportista(entrega.getTelefTransportista())
            .estado(entrega.getEstado() != null ? entrega.getEstado().name() : null)
            .fechaCreacion(entrega.getFechaCreacion())
            .fechaSalida(entrega.getFechaSalida())
            .fechaEntregaEstimada(entrega.getFechaEntregaEstimada())
            .fechaEntregaReal(entrega.getFechaEntregaReal())
            .recibidoPor(entrega.getRecibidoPor())
            .conformidad(entrega.getConformidad())
            .observaciones(entrega.getObservaciones())
            .build();
    }
}
