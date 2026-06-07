package com.madera.reportes.dto.externos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaDto {
    private Long id;
    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String minaDestino;
    private String transportista;
    private String vehiculo;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
    private Boolean conformidad;
}
