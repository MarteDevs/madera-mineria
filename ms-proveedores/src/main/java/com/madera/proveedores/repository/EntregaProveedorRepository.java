package com.madera.proveedores.repository;

import com.madera.proveedores.model.EntregaProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EntregaProveedorRepository extends JpaRepository<EntregaProveedor, Long> {

    List<EntregaProveedor> findByProveedorId(Long proveedorId);
    List<EntregaProveedor> findByTipoMadera(String tipoMadera);
    List<EntregaProveedor> findByFechaEntregaBetween(
        LocalDateTime inicio, LocalDateTime fin);
    List<EntregaProveedor> findByStockActualizado(Boolean actualizado);

    // Total entregado por proveedor
    @Query("SELECT SUM(e.cantidadEntregada) FROM EntregaProveedor e " +
           "WHERE e.proveedor.id = :proveedorId")
    Integer totalEntregadoPorProveedor(@Param("proveedorId") Long proveedorId);
}
