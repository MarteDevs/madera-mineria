package com.madera.proveedores.service;

import com.madera.proveedores.client.InventarioClient;
import com.madera.proveedores.config.RabbitMQConfig;
import com.madera.proveedores.dto.*;
import com.madera.proveedores.model.Contrato;
import com.madera.proveedores.model.EntregaProveedor;
import com.madera.proveedores.model.EstadoContrato;
import com.madera.proveedores.model.Proveedor;
import com.madera.proveedores.repository.ContratoRepository;
import com.madera.proveedores.repository.EntregaProveedorRepository;
import com.madera.proveedores.repository.ProveedorRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProveedorServiceImpl implements ProveedorService {

    private final ProveedorRepository         proveedorRepo;
    private final ContratoRepository          contratoRepo;
    private final EntregaProveedorRepository  entregaRepo;
    private final InventarioClient            inventarioClient;
    private final AmqpTemplate                amqpTemplate;

    // ─── CREAR PROVEEDOR ─────────────────────────────────────────────────────
    @Override
    @Transactional
    public ProveedorResponse crearProveedor(ProveedorRequest request) {

        if (proveedorRepo.existsByRuc(request.getRuc())) {
            throw new RuntimeException(
                "Ya existe un proveedor con el RUC: " + request.getRuc());
        }

        Proveedor proveedor = Proveedor.builder()
            .ruc(request.getRuc())
            .razonSocial(request.getRazonSocial())
            .nombreComercial(request.getNombreComercial())
            .contactoNombre(request.getContactoNombre())
            .contactoEmail(request.getContactoEmail())
            .contactoTelefono(request.getContactoTelefono())
            .direccion(request.getDireccion())
            .ciudad(request.getCiudad())
            .activo(true)
            .build();

        proveedorRepo.save(proveedor);
        return mapToResponse(proveedor);
    }

    // ─── REGISTRAR ENTREGA ───────────────────────────────────────────────────
    @Override
    @Transactional
    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackRegistrarEntrega")
    @Retry(name = "inventario")
    public EntregaProveedorResponse registrarEntrega(
            Long proveedorId, EntregaProveedorRequest request) {

        // 1. Verificar que el proveedor existe
        Proveedor proveedor = proveedorRepo.findById(proveedorId)
            .orElseThrow(() -> new RuntimeException(
                "Proveedor no encontrado: " + proveedorId));

        // 2. Verificar contrato vigente
        Contrato contrato = contratoRepo
            .findByProveedorIdAndEstado(proveedorId, EstadoContrato.VIGENTE)
            .stream()
            .filter(c -> c.getTipoMadera().equalsIgnoreCase(request.getTipoMadera()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(
                "No hay contrato vigente para el tipo de madera: " +
                request.getTipoMadera()));

        // 3. Calcular monto total
        double montoTotal = contrato.getPrecioPactado()
            * request.getCantidadEntregada();

        // 4. Crear registro de entrega
        EntregaProveedor entrega = EntregaProveedor.builder()
            .proveedor(proveedor)
            .contrato(contrato)
            .maderaId(request.getMaderaId())
            .tipoMadera(request.getTipoMadera())
            .cantidadEntregada(request.getCantidadEntregada())
            .unidad(request.getUnidad())
            .precioUnitario(contrato.getPrecioPactado())
            .montoTotal(montoTotal)
            .guiaRemision(request.getGuiaRemision())
            .responsableRecepcion(request.getResponsableRecepcion())
            .stockActualizado(false)
            .build();

        entregaRepo.save(entrega);

        // 5. Actualizar stock en ms-inventario vía Feign (con 4 parámetros corregidos)
        inventarioClient.registrarEntrada(
            request.getMaderaId(),
            request.getCantidadEntregada(),
            "Entrega proveedor: " + proveedor.getRazonSocial() + " | Guía: " + request.getGuiaRemision(),
            request.getResponsableRecepcion()
        );

        entrega.setStockActualizado(true);
        entregaRepo.save(entrega);

        // 6. Publicar evento en RabbitMQ
        EntregaEvent evento = new EntregaEvent(
            entrega.getId(),
            proveedor.getRazonSocial(),
            request.getTipoMadera(),
            request.getCantidadEntregada(),
            LocalDateTime.now()
        );
        amqpTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            RabbitMQConfig.ROUTING_PROVEEDOR,
            evento
        );

        log.info("Entrega registrada: {} unidades de {} del proveedor {}",
            request.getCantidadEntregada(),
            request.getTipoMadera(),
            proveedor.getRazonSocial());

        return mapEntregaToResponse(entrega);
    }

    // ─── FALLBACK ────────────────────────────────────────────────────────────
    public EntregaProveedorResponse fallbackRegistrarEntrega(
            Long proveedorId, EntregaProveedorRequest request, Exception ex) {

        log.error("Circuit Breaker activo. ms-inventario no disponible. " +
            "Entrega registrada pero stock NO actualizado. Motivo: {}", ex.getMessage());

        // La entrega se guarda igual, pero stock no se actualiza
        // Se puede reprocesar después
        throw new RuntimeException(
            "La entrega fue registrada pero el stock no pudo actualizarse en el inventario. " +
            "Detalle: " + ex.getMessage());
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    @Override
    public List<ProveedorResponse> listarTodos() {
        return proveedorRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    public List<ProveedorResponse> listarActivos() {
        return proveedorRepo.findByActivo(true).stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    public ProveedorResponse obtenerPorId(Long id) {
        return mapToResponse(proveedorRepo.findById(id)
            .orElseThrow(() -> new RuntimeException(
                "Proveedor no encontrado: " + id)));
    }

    @Override
    public List<EntregaProveedorResponse> entregasPorProveedor(Long id) {
        return entregaRepo.findByProveedorId(id).stream()
            .map(this::mapEntregaToResponse).toList();
    }

    @Override
    public List<ContratoResponse> contratosPorProveedor(Long id) {
        return contratoRepo.findByProveedorId(id).stream()
            .map(this::mapContratoToResponse).toList();
    }

    @Override
    public List<ContratoResponse> contratosProximosAVencer() {
        return contratoRepo.findContratosProximosAVencer(
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        ).stream().map(this::mapContratoToResponse).toList();
    }

    // ─── MAPPERS ─────────────────────────────────────────────────────────────
    private ProveedorResponse mapToResponse(Proveedor p) {
        ProveedorResponse r = new ProveedorResponse();
        r.setId(p.getId());
        r.setRuc(p.getRuc());
        r.setRazonSocial(p.getRazonSocial());
        r.setNombreComercial(p.getNombreComercial());
        r.setContactoNombre(p.getContactoNombre());
        r.setContactoEmail(p.getContactoEmail());
        r.setContactoTelefono(p.getContactoTelefono());
        r.setDireccion(p.getDireccion());
        r.setCiudad(p.getCiudad());
        r.setActivo(p.getActivo());
        r.setFechaRegistro(p.getFechaRegistro());
        return r;
    }

    private EntregaProveedorResponse mapEntregaToResponse(EntregaProveedor e) {
        EntregaProveedorResponse r = new EntregaProveedorResponse();
        r.setId(e.getId());
        r.setProveedorNombre(e.getProveedor().getRazonSocial());
        r.setTipoMadera(e.getTipoMadera());
        r.setCantidadEntregada(e.getCantidadEntregada());
        r.setUnidad(e.getUnidad());
        r.setPrecioUnitario(e.getPrecioUnitario());
        r.setMontoTotal(e.getMontoTotal());
        r.setGuiaRemision(e.getGuiaRemision());
        r.setStockActualizado(e.getStockActualizado());
        r.setFechaEntrega(e.getFechaEntrega());
        return r;
    }

    private ContratoResponse mapContratoToResponse(Contrato c) {
        ContratoResponse r = new ContratoResponse();
        r.setId(c.getId());
        r.setProveedorNombre(c.getProveedor().getRazonSocial());
        r.setTipoMadera(c.getTipoMadera());
        r.setUnidad(c.getUnidad());
        r.setPrecioPactado(c.getPrecioPactado());
        r.setCantidadMinima(c.getCantidadMinima());
        r.setCantidadMaxima(c.getCantidadMaxima());
        r.setFechaInicio(c.getFechaInicio());
        r.setFechaFin(c.getFechaFin());
        r.setEstado(c.getEstado().name());
        r.setObservaciones(c.getObservaciones());
        r.setFechaCreacion(c.getFechaCreacion());
        return r;
    }
}
