package com.madera.proveedores.repository;

import com.madera.proveedores.model.Contrato;
import com.madera.proveedores.model.EstadoContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {
    List<Contrato> findByProveedorId(Long proveedorId);
    List<Contrato> findByEstado(EstadoContrato estado);
    List<Contrato> findByTipoMadera(String tipoMadera);
    List<Contrato> findByProveedorIdAndEstado(Long proveedorId, EstadoContrato estado);

    // Verificar contratos próximos a vencer (en los próximos 30 días)
    @Query("SELECT c FROM Contrato c WHERE c.estado = 'VIGENTE' " +
           "AND c.fechaFin BETWEEN :hoy AND :limite")
    List<Contrato> findContratosProximosAVencer(
        @Param("hoy") LocalDate hoy,
        @Param("limite") LocalDate limite);
}
