package com.madera.notificaciones.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String areaSolicitante;

    private String tipoEvento;
    private String mensaje;

    @Enumerated(EnumType.STRING)
    private EstadoNotificacion estado;

    private String destinatario;

    @CreationTimestamp
    private LocalDateTime fechaRecibida;

    private LocalDateTime fechaLeida;
}
