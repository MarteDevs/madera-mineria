package com.madera.reportes.dto.response;

import com.madera.reportes.dto.externos.EntregaDto;
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
public class ReporteEntregasResponse {
    private Integer totalEntregas;
    private Integer entregasCompletadas;
    private Integer entregasEnTransito;
    private Integer entregasFallidas;
    private Double tiempoPromedioHoras;
    private Double porcentajeConformidad;
    private Map<String, Integer> porMina;
    private Map<String, Integer> porTransportista;
    private List<EntregaDto> detalle;
    private LocalDateTime fechaGeneracion;
}
