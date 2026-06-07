package com.madera.reportes.controller;

import com.madera.reportes.dto.response.*;
import com.madera.reportes.model.ReporteGenerado;
import com.madera.reportes.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    // Dashboard general — consulta los microservicios en paralelo
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestParam(defaultValue = "sistema") String solicitadoPor) {
        return ResponseEntity.ok(reporteService.generarDashboard(solicitadoPor));
    }

    // Reporte de stock por mina (o todo si no se especifica)
    @GetMapping("/stock")
    public ResponseEntity<ReporteStockResponse> stock(
            @RequestParam(required = false) String mina) {
        return ResponseEntity.ok(reporteService.reporteStock(mina));
    }

    // Reporte de pedidos con filtros opcionales de fecha flexibles
    @GetMapping("/pedidos")
    public ResponseEntity<ReportePedidosResponse> pedidos(
            @RequestParam(required = false) String mina,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        
        LocalDateTime fechaDesde = null;
        LocalDateTime fechaHasta = null;

        if (desde != null && !desde.isBlank()) {
            fechaDesde = parseFecha(desde, true);
        }
        if (hasta != null && !hasta.isBlank()) {
            fechaHasta = parseFecha(hasta, false);
        }

        return ResponseEntity.ok(reporteService.reportePedidos(mina, fechaDesde, fechaHasta));
    }

    // Reporte de entregas
    @GetMapping("/entregas")
    public ResponseEntity<ReporteEntregasResponse> entregas(
            @RequestParam(required = false) String mina) {
        return ResponseEntity.ok(reporteService.reporteEntregas(mina));
    }

    // Historial de reportes generados
    @GetMapping("/historial")
    public ResponseEntity<List<ReporteGenerado>> historial() {
        return ResponseEntity.ok(reporteService.historial());
    }

    // Método seguro para parsear fechas con o sin tiempo
    private LocalDateTime parseFecha(String valor, boolean esInicioDia) {
        try {
            if (valor.contains("T")) {
                return LocalDateTime.parse(valor);
            } else {
                java.time.LocalDate ld = java.time.LocalDate.parse(valor);
                return esInicioDia ? ld.atStartOfDay() : ld.atTime(23, 59, 59);
            }
        } catch (Exception e) {
            throw new RuntimeException("Formato de fecha inválido: '" + valor + "'. Use YYYY-MM-DD o YYYY-MM-DDTHH:MM:SS");
        }
    }
}
