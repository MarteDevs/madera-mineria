package com.madera.pedidos.repository;

import com.madera.pedidos.model.EstadoPedido;
import com.madera.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByMina(String mina);
    List<Pedido> findByEstado(EstadoPedido estado);
    List<Pedido> findBySolicitadoPor(String usuario);
    List<Pedido> findByMinaAndEstado(String mina, EstadoPedido estado);

    // Para reportes: pedidos en un rango de fechas
    List<Pedido> findByFechaPedidoBetween(
        LocalDateTime inicio, LocalDateTime fin);

    // Cuántos pedidos pendientes tiene una mina
    long countByMinaAndEstado(String mina, EstadoPedido estado);
}
