package com.madera.pedidos.service;

import com.madera.pedidos.client.InventarioClient;
import com.madera.pedidos.config.RabbitMQConfig;
import com.madera.pedidos.dto.PedidoAprobadoEvent;
import com.madera.pedidos.dto.PedidoEvent;
import com.madera.pedidos.dto.PedidoRequest;
import com.madera.pedidos.dto.PedidoResponse;
import com.madera.pedidos.exception.PedidoNotFoundException;
import com.madera.pedidos.exception.StockInsuficienteException;
import com.madera.pedidos.model.EstadoPedido;
import com.madera.pedidos.model.Pedido;
import com.madera.pedidos.repository.PedidoRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final InventarioClient inventarioClient;
    private final AmqpTemplate amqpTemplate;

    @Override
    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackCrearPedido")
    @Retry(name = "inventario")
    public PedidoResponse crearPedido(PedidoRequest request) {
        // 1. Consultar stock en ms-inventario vía Feign
        Integer stock = inventarioClient.consultarStock(request.getMaderaId());

        if (stock < request.getCantidadSolicitada()) {
            throw new StockInsuficienteException(
                "Stock disponible: " + stock +
                ", solicitado: " + request.getCantidadSolicitada());
        }

        // 2. Crear y guardar el pedido
        Pedido pedido = new Pedido();
        pedido.setMaderaId(request.getMaderaId());
        pedido.setTipoMadera(request.getTipoMadera());
        pedido.setUsoMadera(request.getUsoMadera());
        pedido.setCantidadSolicitada(request.getCantidadSolicitada());
        pedido.setUnidad(request.getUnidad());
        pedido.setMina(request.getMina());
        pedido.setAreaSolicitante(request.getAreaSolicitante());
        pedido.setSolicitadoPor(request.getSolicitadoPor());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setFechaEntregaEstimada(LocalDateTime.now().plusDays(3));
        
        pedido = pedidoRepo.save(pedido);

        // 3. Publicar evento en RabbitMQ (asíncrono)
        PedidoEvent evento = new PedidoEvent(
            pedido.getId(),
            pedido.getTipoMadera(),
            pedido.getCantidadSolicitada(),
            pedido.getMina(),
            "CREADO",
            LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_KEY,
            evento
        );

        return mapToResponse(pedido);
    }

    // ─── FALLBACK — se ejecuta cuando el Circuit Breaker está ABIERTO ────────
    public PedidoResponse fallbackCrearPedido(
            PedidoRequest request, Exception ex) {

        log.error("Circuit Breaker activo para ms-inventario (crear). " +
            "Causa: {}", ex.getMessage());

        throw new RuntimeException(
            "El servicio de inventario no está disponible temporalmente. " +
            "Por favor intente en unos minutos.");
    }

    @Override
    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackAprobarPedido")
    public PedidoResponse aprobarPedido(Long id, String aprobadoPor) {
        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalStateException(
                "Solo se pueden aprobar pedidos en estado PENDIENTE");
        }

        // Reducir stock en ms-inventario vía Feign
        inventarioClient.reducirStock(
            pedido.getMaderaId(),
            pedido.getCantidadSolicitada()
        );

        pedido.setEstado(EstadoPedido.APROBADO);
        pedido.setAprobadoPor(aprobadoPor);
        pedido.setFechaAprobacion(LocalDateTime.now());
        pedido = pedidoRepo.save(pedido);

        PedidoAprobadoEvent eventoAprobado = new PedidoAprobadoEvent(
            pedido.getId(),
            pedido.getTipoMadera(),
            pedido.getCantidadSolicitada(),
            pedido.getUnidad(),
            pedido.getMina(),
            pedido.getSolicitadoPor(),
            LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            "pedido.aprobado",
            eventoAprobado
        );

        return mapToResponse(pedido);
    }

    // ─── FALLBACK para aprobar ───────────────────────────────────────────────
    public PedidoResponse fallbackAprobarPedido(Long id,
            String aprobadoPor, Exception ex) {

        log.error("No se pudo reducir stock. Circuit Breaker activo. " +
            "Pedido #{}", id);

        throw new RuntimeException(
            "No se pudo comunicar con el servicio de inventario. " +
            "El pedido no fue aprobado.");
    }

    @Override
    @Transactional
    public PedidoResponse rechazarPedido(Long id, String motivo) {
        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        pedido.setEstado(EstadoPedido.RECHAZADO);
        pedido.setMotivoRechazo(motivo);
        pedido = pedidoRepo.save(pedido);

        return mapToResponse(pedido);
    }

    @Override
    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));

        pedido.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoPedido.ENTREGADO) {
            pedido.setFechaEntregaReal(LocalDateTime.now());
        }

        pedido = pedidoRepo.save(pedido);
        return mapToResponse(pedido);
    }

    @Override
    public List<PedidoResponse> listarTodos() {
        return pedidoRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    public List<PedidoResponse> listarPorMina(String mina) {
        return pedidoRepo.findByMina(mina).stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    public List<PedidoResponse> listarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado).stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    public PedidoResponse obtenerPorId(Long id) {
        Pedido pedido = pedidoRepo.findById(id)
            .orElseThrow(() -> new PedidoNotFoundException("Pedido no encontrado: " + id));
        return mapToResponse(pedido);
    }

    private PedidoResponse mapToResponse(Pedido p) {
        PedidoResponse r = new PedidoResponse();
        r.setId(p.getId());
        r.setTipoMadera(p.getTipoMadera());
        r.setCantidadSolicitada(p.getCantidadSolicitada());
        r.setMina(p.getMina());
        r.setEstado(p.getEstado().name());
        r.setFechaPedido(p.getFechaPedido());
        r.setFechaEntregaEstimada(p.getFechaEntregaEstimada());
        return r;
    }
}
