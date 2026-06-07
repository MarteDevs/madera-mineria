package com.madera.mantenimiento.service;

import com.madera.mantenimiento.dto.*;
import com.madera.mantenimiento.model.*;
import com.madera.mantenimiento.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MantenimientoServiceImpl implements MantenimientoService {

    private final VehiculoRepository vehiculoRepo;
    private final RegistroMantenimientoRepository registroRepo;
    private final AlertaMantenimientoRepository alertaRepo;

    @Value("${mantenimiento.km-limite:5000}")
    private Double kmLimite;

    @Value("${mantenimiento.dias-limite:90}")
    private Integer diasLimite;

    // ─── REGISTRAR VEHÍCULO ──────────────────────────────────────────────────
    @Override
    @Transactional
    public VehiculoResponse registrarVehiculo(VehiculoRequest request) {
        String placaNormalizada = request.getPlaca().trim().toUpperCase();
        if (vehiculoRepo.existsByPlaca(placaNormalizada)) {
            throw new RuntimeException("Ya existe un vehículo registrado con la placa: " + placaNormalizada);
        }

        double kmInicial = request.getKmActual() != null ? request.getKmActual() : 0.0;

        Vehiculo vehiculo = Vehiculo.builder()
            .placa(placaNormalizada)
            .marca(request.getMarca())
            .modelo(request.getModelo())
            .anio(request.getAnio())
            .tipo(request.getTipo())
            .capacidadToneladasM3(request.getCapacidadToneladasM3())
            .conductorNombre(request.getConductorNombre())
            .conductorLicencia(request.getConductorLicencia())
            .conductorTelefono(request.getConductorTelefono())
            .kmActual(kmInicial)
            .kmUltimoMantenimiento(kmInicial)
            .kmProximoMantenimiento(kmInicial + kmLimite)
            .vencimientoSoat(request.getVencimientoSoat())
            .vencimientoRevisionTecnica(request.getVencimientoRevisionTecnica())
            .estado(EstadoVehiculo.OPERATIVO)
            .totalEntregasRealizadas(0)
            .requiereMantenimiento(false)
            .build();

        vehiculo = vehiculoRepo.save(vehiculo);
        log.info("Vehículo {} registrado exitosamente.", placaNormalizada);
        return mapToResponse(vehiculo);
    }

    // ─── PROCESAR EVENTO DE ENTREGA COMPLETADA ───────────────────────────────
    @Override
    @Transactional
    public void procesarEntregaCompletada(EntregaCompletadaEvent evento) {
        if (evento.getPlacaVehiculo() == null || evento.getPlacaVehiculo().isBlank()) {
            log.warn("Evento de entrega completada recibido sin placa de vehículo. Ignorando.");
            return;
        }

        String placaNormalizada = evento.getPlacaVehiculo().trim().toUpperCase();

        // Si el vehículo no existe en la base de datos de mantenimiento, se crea automáticamente
        Vehiculo vehiculo = vehiculoRepo.findByPlaca(placaNormalizada)
            .orElseGet(() -> {
                log.info("Vehículo placa {} no encontrado en el sistema. Creándolo automáticamente.", placaNormalizada);
                return crearVehiculoAutomatico(evento);
            });

        // Actualizar kilometraje y total de entregas
        double kmRecorridos = evento.getDistanciaKm() != null ? evento.getDistanciaKm() : 0.0;
        vehiculo.setKmActual(vehiculo.getKmActual() + kmRecorridos);
        vehiculo.setTotalEntregasRealizadas(vehiculo.getTotalEntregasRealizadas() + 1);

        // Verificar si excede el límite de km programado para el mantenimiento
        if (vehiculo.getKmActual() >= vehiculo.getKmProximoMantenimiento()) {
            vehiculo.setRequiereMantenimiento(true);
            generarAlerta(vehiculo, "KM_EXCEDIDO",
                String.format("El vehículo %s superó el kilometraje límite (Límite: %.1f km | Actual: %.1f km).",
                    vehiculo.getPlaca(), vehiculo.getKmProximoMantenimiento(), vehiculo.getKmActual()),
                "ALTA");
        }

        // Verificar si el SOAT está por vencer (menor o igual a 30 días)
        if (vehiculo.getVencimientoSoat() != null) {
            long diasRestantesSoat = ChronoUnit.DAYS.between(LocalDate.now(), vehiculo.getVencimientoSoat());
            if (diasRestantesSoat <= 30) {
                String prioridad = diasRestantesSoat <= 7 ? "ALTA" : "MEDIA";
                generarAlerta(vehiculo, "SOAT_POR_VENCER",
                    String.format("El SOAT del vehículo %s vence en %d días (Vence: %s).",
                        vehiculo.getPlaca(), diasRestantesSoat, vehiculo.getVencimientoSoat()),
                    prioridad);
            }
        }

        // Verificar si la Revisión Técnica está por vencer (menor o igual a 30 días)
        if (vehiculo.getVencimientoRevisionTecnica() != null) {
            long diasRestantesRT = ChronoUnit.DAYS.between(LocalDate.now(), vehiculo.getVencimientoRevisionTecnica());
            if (diasRestantesRT <= 30) {
                String prioridad = diasRestantesRT <= 7 ? "ALTA" : "MEDIA";
                generarAlerta(vehiculo, "REVISION_POR_VENCER",
                    String.format("La revisión técnica del vehículo %s vence en %d días (Vence: %s).",
                        vehiculo.getPlaca(), diasRestantesRT, vehiculo.getVencimientoRevisionTecnica()),
                    prioridad);
            }
        }

        vehiculoRepo.save(vehiculo);
    }

    // ─── REGISTRAR MANTENIMIENTO ─────────────────────────────────────────────
    @Override
    @Transactional
    public RegistroMantenimientoResponse registrarMantenimiento(Long vehiculoId, RegistroMantenimientoRequest request) {
        Vehiculo vehiculo = vehiculoRepo.findById(vehiculoId)
            .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con ID: " + vehiculoId));

        RegistroMantenimiento registro = RegistroMantenimiento.builder()
            .vehiculo(vehiculo)
            .tipo(request.getTipo())
            .descripcion(request.getDescripcion())
            .taller(request.getTaller())
            .kmAlServicio(vehiculo.getKmActual())
            .costo(request.getCosto() != null ? request.getCosto() : 0.0)
            .tecnicoResponsable(request.getTecnicoResponsable())
            .observaciones(request.getObservaciones())
            .fechaIngreso(request.getFechaIngreso() != null ? request.getFechaIngreso() : LocalDate.now())
            .fechaSalida(request.getFechaSalida())
            .build();

        registro = registroRepo.save(registro);

        // Actualizar vehículo posterior al servicio
        vehiculo.setKmUltimoMantenimiento(vehiculo.getKmActual());
        vehiculo.setKmProximoMantenimiento(vehiculo.getKmActual() + kmLimite);
        vehiculo.setFechaUltimoMantenimiento(LocalDate.now());
        vehiculo.setFechaProximoMantenimiento(LocalDate.now().plusDays(diasLimite));
        vehiculo.setRequiereMantenimiento(false);

        // Si el vehículo aún no sale del taller, queda en estado EN_MANTENIMIENTO
        if (request.getFechaSalida() != null) {
            vehiculo.setEstado(EstadoVehiculo.OPERATIVO);
        } else {
            vehiculo.setEstado(EstadoVehiculo.EN_MANTENIMIENTO);
        }
        vehiculoRepo.save(vehiculo);

        // Resolver automáticamente las alertas pendientes de este vehículo
        List<AlertaMantenimiento> alertasPendientes = alertaRepo.findByVehiculoId(vehiculoId);
        for (AlertaMantenimiento alerta : alertasPendientes) {
            if (!alerta.getResuelta()) {
                alerta.setResuelta(true);
                alerta.setFechaResolucion(LocalDateTime.now());
                alertaRepo.save(alerta);
            }
        }

        log.info("Servicio de mantenimiento registrado para el vehículo Placa: {}.", vehiculo.getPlaca());
        return mapRegistroToResponse(registro);
    }

    // ─── CONSULTAS ───────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarVehiculos() {
        return vehiculoRepo.findAll().stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehiculoResponse> vehiculosQueRequierenMantenimiento() {
        return vehiculoRepo.findByRequiereMantenimiento(true).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertaMantenimientoResponse> alertasPendientes() {
        return alertaRepo.findByResuelta(false).stream()
            .map(this::mapAlertaToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroMantenimientoResponse> historialVehiculo(Long id) {
        return registroRepo.findByVehiculoId(id).stream()
            .map(this::mapRegistroToResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculoResponse obtenerPorPlaca(String placa) {
        Vehiculo vehiculo = vehiculoRepo.findByPlaca(placa.toUpperCase())
            .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con placa: " + placa));
        return mapToResponse(vehiculo);
    }

    // ─── HELPERS PRIVADOS Y MAPPERS ──────────────────────────────────────────

    private Vehiculo crearVehiculoAutomatico(EntregaCompletadaEvent evento) {
        Vehiculo v = Vehiculo.builder()
            .placa(evento.getPlacaVehiculo().toUpperCase())
            .marca("Por registrar")
            .modelo("Por registrar")
            .anio(LocalDate.now().getYear())
            .tipo("Camion")
            .capacidadToneladasM3(15.0)
            .conductorNombre(evento.getConductorNombre())
            .kmActual(0.0)
            .kmUltimoMantenimiento(0.0)
            .kmProximoMantenimiento(kmLimite)
            .estado(EstadoVehiculo.OPERATIVO)
            .totalEntregasRealizadas(0)
            .requiereMantenimiento(false)
            .build();
        return vehiculoRepo.save(v);
    }

    private void generarAlerta(Vehiculo vehiculo, String tipo, String mensaje, String prioridad) {
        // Evitar duplicar alertas del mismo tipo que aún no están resueltas
        boolean existe = alertaRepo.findByVehiculoId(vehiculo.getId()).stream()
            .anyMatch(a -> a.getTipoAlerta().equals(tipo) && !a.getResuelta());
        
        if (!existe) {
            AlertaMantenimiento alerta = AlertaMantenimiento.builder()
                .vehiculo(vehiculo)
                .tipoAlerta(tipo)
                .mensaje(mensaje)
                .prioridad(prioridad)
                .resuelta(false)
                .build();
            alertaRepo.save(alerta);
            log.info("Alerta de tipo {} generada para vehículo placa: {}", tipo, vehiculo.getPlaca());
        }
    }

    private VehiculoResponse mapToResponse(Vehiculo v) {
        double kmRestantes = v.getKmProximoMantenimiento() != null
            ? Math.max(0.0, v.getKmProximoMantenimiento() - v.getKmActual())
            : 0.0;

        return VehiculoResponse.builder()
            .id(v.getId())
            .placa(v.getPlaca())
            .marca(v.getMarca())
            .modelo(v.getModelo())
            .tipo(v.getTipo())
            .conductorNombre(v.getConductorNombre())
            .conductorLicencia(v.getConductorLicencia())
            .conductorTelefono(v.getConductorTelefono())
            .kmActual(v.getKmActual())
            .kmProximoMantenimiento(v.getKmProximoMantenimiento())
            .kmRestantesParaMantenimiento(kmRestantes)
            .fechaUltimoMantenimiento(v.getFechaUltimoMantenimiento())
            .fechaProximoMantenimiento(v.getFechaProximoMantenimiento())
            .vencimientoSoat(v.getVencimientoSoat())
            .vencimientoRevisionTecnica(v.getVencimientoRevisionTecnica())
            .estado(v.getEstado() != null ? v.getEstado().name() : null)
            .requiereMantenimiento(v.getRequiereMantenimiento())
            .totalEntregasRealizadas(v.getTotalEntregasRealizadas())
            .build();
    }

    private RegistroMantenimientoResponse mapRegistroToResponse(RegistroMantenimiento r) {
        return RegistroMantenimientoResponse.builder()
            .id(r.getId())
            .placaVehiculo(r.getVehiculo().getPlaca())
            .tipo(r.getTipo() != null ? r.getTipo().name() : null)
            .descripcion(r.getDescripcion())
            .taller(r.getTaller())
            .kmAlServicio(r.getKmAlServicio())
            .costo(r.getCosto())
            .tecnicoResponsable(r.getTecnicoResponsable())
            .observaciones(r.getObservaciones())
            .fechaIngreso(r.getFechaIngreso())
            .fechaSalida(r.getFechaSalida())
            .fechaRegistro(r.getFechaRegistro())
            .build();
    }

    private AlertaMantenimientoResponse mapAlertaToResponse(AlertaMantenimiento a) {
        return AlertaMantenimientoResponse.builder()
            .id(a.getId())
            .vehiculoId(a.getVehiculo().getId())
            .placaVehiculo(a.getVehiculo().getPlaca())
            .tipoAlerta(a.getTipoAlerta())
            .mensaje(a.getMensaje())
            .prioridad(a.getPrioridad())
            .resuelta(a.getResuelta())
            .fechaGeneracion(a.getFechaGeneracion())
            .fechaResolucion(a.getFechaResolucion())
            .build();
    }
}
