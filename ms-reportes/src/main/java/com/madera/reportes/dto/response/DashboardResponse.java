package com.madera.reportes.dto.response;

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
public class DashboardResponse {

    // Resumen de inventario
    private Integer totalTiposMadera;
    private Integer stockTotalUnidades;
    private List<String> tiposConStockBajo;     // stock < stockMinimo
    private Map<String, Integer> stockPorMina;  // mina → total unidades

    // Resumen de pedidos
    private Integer totalPedidos;
    private Integer pedidosPendientes;
    private Integer pedidosAprobados;
    private Integer pedidosEntregados;
    private Integer pedidosRechazados;
    private Map<String, Integer> pedidosPorMina;

    // Resumen de entregas
    private Integer totalEntregas;
    private Integer entregasEnTransito;
    private Integer entregasCompletadas;
    private Double tiempoPromedioEntregaHoras;

    // Resumen de proveedores
    private Integer totalProveedores;
    private Integer proveedoresActivos;
    private Integer contratosProximosAVencer;

    // Metadata del reporte
    private LocalDateTime fechaGeneracion;
    private Long tiempoGeneracionMs;
    private List<String> serviciosNoDisponibles;
}
