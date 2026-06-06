package com.madera.proveedores.repository;

import com.madera.proveedores.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByRuc(String ruc);
    boolean existsByRuc(String ruc);
    List<Proveedor> findByActivo(Boolean activo);
    List<Proveedor> findByRazonSocialContainingIgnoreCase(String nombre);
}
