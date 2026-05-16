package com.madera.entrega.repository;

import com.madera.entrega.model.HistorialEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialEntregaRepository extends JpaRepository<HistorialEntrega, Long> {
}
