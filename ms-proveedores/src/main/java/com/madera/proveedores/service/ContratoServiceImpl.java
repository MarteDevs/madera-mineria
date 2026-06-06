package com.madera.proveedores.service;

import com.madera.proveedores.dto.ContratoRequest;
import com.madera.proveedores.dto.ContratoResponse;
import com.madera.proveedores.model.Contrato;
import com.madera.proveedores.model.EstadoContrato;
import com.madera.proveedores.model.Proveedor;
import com.madera.proveedores.repository.ContratoRepository;
import com.madera.proveedores.repository.ProveedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoService {

    private final ContratoRepository contratoRepo;
    private final ProveedorRepository proveedorRepo;

    @Override
    @Transactional
    public ContratoResponse crearContrato(Long proveedorId, ContratoRequest request) {
        Proveedor proveedor = proveedorRepo.findById(proveedorId)
            .orElseThrow(() -> new RuntimeException("Proveedor no encontrado: " + proveedorId));

        EstadoContrato estado = EstadoContrato.VIGENTE;
        if (request.getEstado() != null) {
            try {
                estado = EstadoContrato.valueOf(request.getEstado().toUpperCase());
            } catch (IllegalArgumentException e) {
                // usar por defecto VIGENTE
            }
        }

        Contrato contrato = Contrato.builder()
            .proveedor(proveedor)
            .tipoMadera(request.getTipoMadera())
            .unidad(request.getUnidad())
            .precioPactado(request.getPrecioPactado())
            .cantidadMinima(request.getCantidadMinima())
            .cantidadMaxima(request.getCantidadMaxima())
            .fechaInicio(request.getFechaInicio())
            .fechaFin(request.getFechaFin())
            .estado(estado)
            .observaciones(request.getObservaciones())
            .build();

        contratoRepo.save(contrato);
        return mapToResponse(contrato);
    }

    @Override
    public List<ContratoResponse> listarTodosContratos() {
        return contratoRepo.findAll().stream()
            .map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public ContratoResponse actualizarEstadoContrato(Long contratoId, String nuevoEstado) {
        Contrato contrato = contratoRepo.findById(contratoId)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + contratoId));

        try {
            EstadoContrato estado = EstadoContrato.valueOf(nuevoEstado.toUpperCase());
            contrato.setEstado(estado);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de contrato inválido: " + nuevoEstado);
        }

        contratoRepo.save(contrato);
        return mapToResponse(contrato);
    }

    @Override
    public ContratoResponse obtenerContratoPorId(Long id) {
        Contrato contrato = contratoRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Contrato no encontrado: " + id));
        return mapToResponse(contrato);
    }

    private ContratoResponse mapToResponse(Contrato c) {
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
