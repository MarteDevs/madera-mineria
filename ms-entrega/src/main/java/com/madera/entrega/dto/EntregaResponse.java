package com.madera.entrega.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaResponse {
    private Long id;
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String unidad;
    private String almacenOrigen;
    private String minaDestino;
    private String direccionMina;
    private Double distanciaKm;
    private String transportista;
    private String vehiculo;
    private String telefTransportista;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
    private String recibidoPor;
    private Boolean conformidad;
    private String observaciones;
}
