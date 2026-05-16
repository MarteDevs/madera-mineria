package com.madera.ms_inventario.repository;

import com.madera.ms_inventario.model.Madera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaderaRepository extends JpaRepository<Madera, Long> {
    List<Madera> findByMina(String mina);
    List<Madera> findByEstado(String estado);
    Optional<Madera> findByTipoAndMina(String tipo, String mina);
    List<Madera> findByStockDisponibleGreaterThan(Integer minimo);
    List<Madera> findByTipo(String tipo);
}
