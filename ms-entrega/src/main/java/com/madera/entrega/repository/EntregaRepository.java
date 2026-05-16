package com.madera.entrega.repository;

import com.madera.entrega.model.Entrega;
import com.madera.entrega.model.EstadoEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntregaRepository extends JpaRepository<Entrega, Long> {

    Optional<Entrega> findByPedidoId(Long pedidoId);

    List<Entrega> findByEstado(EstadoEntrega estado);

    List<Entrega> findByMinaDestino(String mina);

    List<Entrega> findByTransportista(String transportista);

    List<Entrega> findByMinaDestinoAndEstado(String mina, EstadoEntrega estado);

    List<Entrega> findByEstadoIn(List<EstadoEntrega> estados);
}
