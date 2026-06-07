package com.madera.reportes.service;

import com.madera.reportes.dto.response.*;
import com.madera.reportes.model.ReporteGenerado;
import java.time.LocalDateTime;
import java.util.List;

public interface ReporteService {
    DashboardResponse generarDashboard(String solicitadoPor);
    ReporteStockResponse reporteStock(String mina);
    ReportePedidosResponse reportePedidos(String mina, LocalDateTime desde, LocalDateTime hasta);
    ReporteEntregasResponse reporteEntregas(String mina);
    List<ReporteGenerado> historial();
}
