package com.madera.notificaciones.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.madera.notificaciones.model.EstadoNotificacion;
import com.madera.notificaciones.model.Notificacion;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByMina(String mina);

    List<Notificacion> findByEstado(EstadoNotificacion estado);

    List<Notificacion> findByDestinatario(String destinatario);

    List<Notificacion> findByDestinatarioAndEstado(
            String destinatario, EstadoNotificacion estado);

    List<Notificacion> findByPedidoId(Long pedidoId);

    long countByDestinatarioAndEstado(
            String destinatario, EstadoNotificacion estado);
}
