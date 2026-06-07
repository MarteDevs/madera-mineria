package com.madera.reportes.dto.response;

import com.madera.reportes.dto.externos.PedidoDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportePedidosResponse {
    private LocalDateTime desde;
    private LocalDateTime hasta;
    private String mina;
    private Integer totalPedidos;
    private Map<String, Integer> porEstado;
    private Map<String, Integer> porTipoMadera;
    private Integer totalUnidadesSolicitadas;
    private List<PedidoDto> detalle;
    private LocalDateTime fechaGeneracion;
}
