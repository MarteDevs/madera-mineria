package com.madera.reportes.repository;

import com.madera.reportes.model.ReporteGenerado;
import com.madera.reportes.model.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReporteGeneradoRepository extends JpaRepository<ReporteGenerado, Long> {

    List<ReporteGenerado> findByTipo(TipoReporte tipo);
    List<ReporteGenerado> findBySolicitadoPor(String usuario);
    List<ReporteGenerado> findByExitoso(Boolean exitoso);
    List<ReporteGenerado> findByFechaGeneracionBetween(
        LocalDateTime inicio, LocalDateTime fin);

    // Tiempo promedio de generación por tipo
    @Query("SELECT AVG(r.tiempoGeneracionMs) FROM ReporteGenerado r " +
           "WHERE r.tipo = :tipo AND r.exitoso = true")
    Double promedioTiempoGeneracion(@Param("tipo") TipoReporte tipo);
}
