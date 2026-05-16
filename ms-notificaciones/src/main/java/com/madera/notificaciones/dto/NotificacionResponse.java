package com.madera.notificaciones.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {

    private Long id;
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String mina;
    private String areaSolicitante;
    private String tipoEvento;
    private String mensaje;
    private String estado;
    private String destinatario;
    private LocalDateTime fechaRecibida;
    private LocalDateTime fechaLeida;
}
