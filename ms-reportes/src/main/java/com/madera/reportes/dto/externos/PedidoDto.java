package com.madera.reportes.dto.externos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDto {
    private Long id;
    private String tipoMadera;
    private Integer cantidadSolicitada;
    private String unidad;
    private String mina;
    private String solicitadoPor;
    private String estado;
    private LocalDateTime fechaPedido;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;
}
