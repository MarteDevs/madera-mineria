package com.madera.mantenimiento.repository;

import com.madera.mantenimiento.model.EstadoVehiculo;
import com.madera.mantenimiento.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findByPlaca(String placa);
    
    boolean existsByPlaca(String placa);
    
    List<Vehiculo> findByEstado(EstadoVehiculo estado);
    
    List<Vehiculo> findByRequiereMantenimiento(Boolean requiere);
    
    List<Vehiculo> findByConductorNombreContainingIgnoreCase(String nombre);

    // Vehículos con SOAT próximo a vencer (ej. en 30 días)
    @Query("SELECT v FROM Vehiculo v WHERE v.vencimientoSoat " +
           "BETWEEN :hoy AND :limite AND v.estado != 'INACTIVO'")
    List<Vehiculo> findVehiculosConSoatPorVencer(
        @Param("hoy") LocalDate hoy,
        @Param("limite") LocalDate limite);

    // Vehículos que superaron el límite de km y no están inactivos
    @Query("SELECT v FROM Vehiculo v WHERE v.kmActual >= v.kmProximoMantenimiento " +
           "AND v.estado = 'OPERATIVO'")
    List<Vehiculo> findVehiculosKmExcedido();
}
