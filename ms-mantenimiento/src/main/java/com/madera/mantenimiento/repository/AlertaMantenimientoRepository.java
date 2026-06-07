package com.madera.mantenimiento.repository;

import com.madera.mantenimiento.model.AlertaMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertaMantenimientoRepository extends JpaRepository<AlertaMantenimiento, Long> {

    List<AlertaMantenimiento> findByResuelta(Boolean resuelta);

    List<AlertaMantenimiento> findByVehiculoId(Long vehiculoId);

    List<AlertaMantenimiento> findByPrioridad(String prioridad);

    long countByResuelta(Boolean resuelta);
}
