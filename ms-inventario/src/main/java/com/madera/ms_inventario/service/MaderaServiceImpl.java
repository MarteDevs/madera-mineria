package com.madera.ms_inventario.service;

import com.madera.ms_inventario.dto.MaderaRequest;
import com.madera.ms_inventario.dto.MaderaResponse;
import com.madera.ms_inventario.exception.StockInsuficienteException;
import com.madera.ms_inventario.model.Madera;
import com.madera.ms_inventario.model.MovimientoInventario;
import com.madera.ms_inventario.model.TipoMovimiento;
import com.madera.ms_inventario.repository.MaderaRepository;
import com.madera.ms_inventario.repository.MovimientoInventarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class MaderaServiceImpl implements MaderaService {

    private final MaderaRepository maderaRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public MaderaServiceImpl(MaderaRepository maderaRepository, MovimientoInventarioRepository movimientoRepository) {
        this.maderaRepository = maderaRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    public List<MaderaResponse> listarTodas() {
        return maderaRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MaderaResponse obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        Madera madera = maderaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Madera no encontrada con ID: " + id));
        return mapToResponse(madera);
    }

    @Override
    public List<MaderaResponse> listarPorMina(String mina) {
        return maderaRepository.findByMina(mina).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MaderaResponse> listarPorTipo(String tipo) {
        return maderaRepository.findByTipo(tipo).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Integer consultarStock(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        Madera madera = maderaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Madera no encontrada con ID: " + id));
        return madera.getStockDisponible();
    }

    @Override
    @Transactional
    public MaderaResponse crear(MaderaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud no puede ser nula");
        }
        Madera madera = new Madera();
        mapToEntity(request, madera);
        if (madera.getEstado() == null) {
            madera.setEstado("DISPONIBLE");
        }
        Madera guardada = maderaRepository.save(madera);
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public MaderaResponse actualizar(Long id, MaderaRequest request) {
        if (id == null || request == null) {
            throw new IllegalArgumentException("El ID y la solicitud no pueden ser nulos");
        }
        Madera madera = maderaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Madera no encontrada con ID: " + id));
        mapToEntity(request, madera);
        Madera actualizada = maderaRepository.save(madera);
        return mapToResponse(actualizada);
    }

    @Override
    @Transactional
    public MaderaResponse reducirStock(Long id, Integer cantidad) {
        if (id == null || cantidad == null) {
            throw new IllegalArgumentException("El ID y la cantidad no pueden ser nulos");
        }
        Madera madera = maderaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Madera no encontrada con ID: " + id));

        if (madera.getStockDisponible() < cantidad) {
            throw new StockInsuficienteException("Stock disponible: " + madera.getStockDisponible() + ", solicitado: " + cantidad);
        }

        madera.setStockDisponible(madera.getStockDisponible() - cantidad);
        
        if (madera.getStockDisponible() == 0) {
            madera.setEstado("AGOTADO");
        }

        registrarMovimiento(madera, TipoMovimiento.SALIDA, cantidad, "Reducción de stock", "Sistema");
        
        Madera actualizada = maderaRepository.save(madera);
        return mapToResponse(actualizada);
    }

    @Override
    public List<MovimientoInventario> listarMovimientos(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }
        return movimientoRepository.findByMaderaId(id);
    }

    @Override
    @Transactional
    public MaderaResponse registrarEntrada(Long id, Integer cantidad, String motivo, String responsable) {
        if (id == null || cantidad == null) {
            throw new IllegalArgumentException("El ID y la cantidad no pueden ser nulos");
        }
        Madera madera = maderaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Madera no encontrada con ID: " + id));

        madera.setStockDisponible(madera.getStockDisponible() + cantidad);
        
        if (madera.getStockDisponible() > 0 && "AGOTADO".equals(madera.getEstado())) {
            madera.setEstado("DISPONIBLE");
        }

        registrarMovimiento(madera, TipoMovimiento.ENTRADA, cantidad, motivo, responsable);
        
        Madera actualizada = maderaRepository.save(madera);
        return mapToResponse(actualizada);
    }

    private void registrarMovimiento(Madera madera, TipoMovimiento tipo, Integer cantidad, String motivo, String responsable) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setMadera(madera);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setMotivo(motivo);
        movimiento.setResponsable(responsable);
        movimientoRepository.save(movimiento);
    }

    private MaderaResponse mapToResponse(Madera madera) {
        MaderaResponse response = new MaderaResponse();
        response.setId(madera.getId());
        response.setTipo(madera.getTipo());
        response.setUso(madera.getUso());
        response.setUnidad(madera.getUnidad());
        response.setPrecioPorUnidad(madera.getPrecioPorUnidad());
        response.setStockDisponible(madera.getStockDisponible());
        response.setStockMinimo(madera.getStockMinimo());
        response.setMina(madera.getMina());
        response.setProveedor(madera.getProveedor());
        response.setEstado(madera.getEstado());
        response.setUltimaActualizacion(madera.getUltimaActualizacion());
        return response;
    }

    private void mapToEntity(MaderaRequest request, Madera madera) {
        madera.setTipo(request.getTipo());
        madera.setUso(request.getUso());
        madera.setUnidad(request.getUnidad());
        madera.setPrecioPorUnidad(request.getPrecioPorUnidad());
        madera.setStockDisponible(request.getStockDisponible());
        madera.setStockMinimo(request.getStockMinimo());
        madera.setMina(request.getMina());
        madera.setProveedor(request.getProveedor());
        madera.setEstado(request.getEstado());
    }
}
