package com.madera.mantenimiento.repository;

import com.madera.mantenimiento.model.RegistroMantenimiento;
import com.madera.mantenimiento.model.TipoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistroMantenimientoRepository extends JpaRepository<RegistroMantenimiento, Long> {

    List<RegistroMantenimiento> findByVehiculoId(Long vehiculoId);

    List<RegistroMantenimiento> findByTipo(TipoMantenimiento tipo);

    List<RegistroMantenimiento> findByFechaIngresoBetween(LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(r.costo), 0.0) FROM RegistroMantenimiento r " +
           "WHERE r.vehiculo.id = :vehiculoId")
    Double costoTotalPorVehiculo(@Param("vehiculoId") Long vehiculoId);
}
