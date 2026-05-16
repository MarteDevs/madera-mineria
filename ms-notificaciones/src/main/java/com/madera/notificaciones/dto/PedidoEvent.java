package com.madera.notificaciones.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoEvent {

    private Long pedidoId;
    private String tipoMadera;

    @JsonAlias("cantidadSolicitada")
    private Integer cantidad;

    private String mina;
    private String areaSolicitante;
    private String estado;
    private LocalDateTime fecha;
}
