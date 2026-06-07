package com.madera.reportes.service;

import com.madera.reportes.client.EntregaClient;
import com.madera.reportes.client.InventarioClient;
import com.madera.reportes.client.PedidosClient;
import com.madera.reportes.client.ProveedoresClient;
import com.madera.reportes.dto.externos.EntregaDto;
import com.madera.reportes.dto.externos.MaderaDto;
import com.madera.reportes.dto.externos.PedidoDto;
import com.madera.reportes.dto.externos.ProveedorDto;
import com.madera.reportes.dto.response.*;
import com.madera.reportes.model.ReporteGenerado;
import com.madera.reportes.model.TipoReporte;
import com.madera.reportes.repository.ReporteGeneradoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteServiceImpl implements ReporteService {

    private final ReporteGeneradoRepository reporteRepo;
    private final InventarioClient  inventarioClient;
    private final PedidosClient     pedidosClient;
    private final EntregaClient     entregaClient;
    private final ProveedoresClient proveedoresClient;

    // ─── DASHBOARD GENERAL (EN PARALELO Y ASÍNCRONO) ─────────────────────────
    @Override
    @Transactional
    public DashboardResponse generarDashboard(String solicitadoPor) {
        long inicio = System.currentTimeMillis();
        List<String> serviciosFallidos = Collections.synchronizedList(new ArrayList<>());

        // Consultar todos los servicios en paralelo
        CompletableFuture<List<MaderaDto>> futInventario = CompletableFuture.supplyAsync(() -> fetchInventario(serviciosFallidos));
        CompletableFuture<List<PedidoDto>> futPedidos = CompletableFuture.supplyAsync(() -> fetchPedidos(serviciosFallidos));
        CompletableFuture<List<EntregaDto>> futEntregas = CompletableFuture.supplyAsync(() -> fetchEntregas(serviciosFallidos));
        CompletableFuture<List<ProveedorDto>> futProveedores = CompletableFuture.supplyAsync(() -> fetchProveedores(serviciosFallidos));
        CompletableFuture<List<Object>> futContratosVence = CompletableFuture.supplyAsync(() -> fetchContratosVence(serviciosFallidos));

        // Combinar todas las promesas
        CompletableFuture.allOf(futInventario, futPedidos, futEntregas, futProveedores, futContratosVence).join();

        List<MaderaDto> maderas = futInventario.getNow(Collections.emptyList());
        List<PedidoDto> pedidos = futPedidos.getNow(Collections.emptyList());
        List<EntregaDto> entregas = futEntregas.getNow(Collections.emptyList());
        List<ProveedorDto> proveedores = futProveedores.getNow(Collections.emptyList());
        List<Object> contratosVence = futContratosVence.getNow(Collections.emptyList());

        // 2. Calcular métricas de inventario
        Integer totalTipos = maderas.size();
        Integer stockTotal = maderas.stream()
            .mapToInt(m -> m.getStockDisponible() != null ? m.getStockDisponible() : 0)
            .sum();
        List<String> stockBajo = maderas.stream()
            .filter(m -> m.getStockDisponible() != null &&
                         m.getStockMinimo()     != null &&
                         m.getStockDisponible() <= m.getStockMinimo())
            .map(m -> m.getTipo() + " (" + m.getMina() + ")")
            .toList();
        Map<String, Integer> stockPorMina = maderas.stream()
            .collect(Collectors.groupingBy(
                m -> m.getMina() != null ? m.getMina() : "Sin mina",
                Collectors.summingInt(m -> m.getStockDisponible() != null ? m.getStockDisponible() : 0)));

        // 3. Calcular métricas de pedidos
        Map<String, Integer> porEstado = pedidos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstado() != null ? p.getEstado() : "DESCONOCIDO",
                Collectors.summingInt(p -> 1)));
        Map<String, Integer> pedidosPorMina = pedidos.stream()
            .collect(Collectors.groupingBy(
                p -> p.getMina() != null ? p.getMina() : "Sin mina",
                Collectors.summingInt(p -> 1)));

        // 4. Calcular métricas de entregas
        List<EntregaDto> enTransito = entregas.stream()
            .filter(e -> "EN_RUTA".equals(e.getEstado()) || "PREPARANDO".equals(e.getEstado()))
            .toList();
        List<EntregaDto> completadas = entregas.stream()
            .filter(e -> "ENTREGADO".equals(e.getEstado()))
            .toList();

        double tiempoPromedio = completadas.stream()
            .filter(e -> e.getFechaSalida() != null && e.getFechaEntregaReal() != null)
            // Filtro de seguridad para evitar duraciones negativas
            .filter(e -> e.getFechaEntregaReal().isAfter(e.getFechaSalida()))
            .mapToLong(e -> Duration.between(e.getFechaSalida(), e.getFechaEntregaReal()).toHours())
            .average().orElse(0.0);

        // 5. Métricas de proveedores
        long provActivos = proveedores.stream()
            .filter(p -> Boolean.TRUE.equals(p.getActivo()))
            .count();

        // 6. Guardar historial
        long tiempoTotal = System.currentTimeMillis() - inicio;
        ReporteGenerado historial = ReporteGenerado.builder()
            .tipo(TipoReporte.DASHBOARD)
            .solicitadoPor(solicitadoPor)
            .tiempoGeneracionMs(tiempoTotal)
            .exitoso(serviciosFallidos.isEmpty())
            .serviciosConsultados("inventario,pedidos,entrega,proveedores")
            .serviciosFallidos(String.join(",", serviciosFallidos))
            .build();
        reporteRepo.save(historial);

        // 7. Construir respuesta
        return DashboardResponse.builder()
            .totalTiposMadera(totalTipos)
            .stockTotalUnidades(stockTotal)
            .tiposConStockBajo(stockBajo)
            .stockPorMina(stockPorMina)
            .totalPedidos(pedidos.size())
            .pedidosPendientes(porEstado.getOrDefault("PENDIENTE", 0))
            .pedidosAprobados(porEstado.getOrDefault("APROBADO", 0))
            .pedidosEntregados(porEstado.getOrDefault("ENTREGADO", 0))
            .pedidosRechazados(porEstado.getOrDefault("RECHAZADO", 0))
            .pedidosPorMina(pedidosPorMina)
            .totalEntregas(entregas.size())
            .entregasEnTransito(enTransito.size())
            .entregasCompletadas(completadas.size())
            .tiempoPromedioEntregaHoras(tiempoPromedio)
            .totalProveedores(proveedores.size())
            .proveedoresActivos((int) provActivos)
            .contratosProximosAVencer(contratosVence.size()) // Corregido mapeo
            .fechaGeneracion(LocalDateTime.now())
            .tiempoGeneracionMs(tiempoTotal)
            .serviciosNoDisponibles(serviciosFallidos)
            .build();
    }

    // ─── REPORTE DE STOCK ────────────────────────────────────────────────────
    @Override
    @Transactional
    public ReporteStockResponse reporteStock(String mina) {
        long inicio = System.currentTimeMillis();
        boolean exitoso = true;
        String fallido = "";
        List<MaderaDto> maderas = new ArrayList<>();

        try {
            maderas = mina != null
                ? inventarioClient.porMina(mina)
                : inventarioClient.listarTodo();
        } catch (Exception e) {
            exitoso = false;
            fallido = "inventario";
            log.error("Error al obtener stock desde el cliente Feign: {}", e.getMessage());
        }

        List<ReporteStockResponse.ItemStockDto> items = maderas.stream()
            .map(m -> new ReporteStockResponse.ItemStockDto(
                m.getTipo(),
                m.getUso(),
                m.getStockDisponible(),
                m.getStockMinimo(),
                m.getStockDisponible() != null &&
                m.getStockMinimo()     != null &&
                m.getStockDisponible() <= m.getStockMinimo(),
                m.getUnidad(),
                m.getPrecioPorUnidad()
            ))
            .toList();

        int total = items.stream()
            .mapToInt(i -> i.getStock() != null ? i.getStock() : 0)
            .sum();
        long stockBajo = items.stream()
            .filter(ReporteStockResponse.ItemStockDto::getStockBajo)
            .count();

        guardarHistorial(TipoReporte.STOCK, "sistema",
            System.currentTimeMillis() - inicio, exitoso, "inventario", fallido);

        return ReporteStockResponse.builder()
            .mina(mina != null ? mina : "TODAS")
            .items(items)
            .totalUnidades(total)
            .itemsConStockBajo((int) stockBajo)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── REPORTE DE PEDIDOS ──────────────────────────────────────────────────
    @Override
    @Transactional
    public ReportePedidosResponse reportePedidos(
            String mina, LocalDateTime desde, LocalDateTime hasta) {

        long inicio = System.currentTimeMillis();
        boolean exitoso = true;
        String fallido = "";
        List<PedidoDto> todos = new ArrayList<>();

        try {
            todos = mina != null
                ? pedidosClient.porMina(mina)
                : pedidosClient.listarTodos();
        } catch (Exception e) {
            exitoso = false;
            fallido = "pedidos";
            log.error("Error al obtener pedidos desde Feign: {}", e.getMessage());
        }

        // Filtrar por fechas si se proporcionan
        List<PedidoDto> filtrados = todos.stream()
            .filter(p -> {
                if (desde == null && hasta == null) return true;
                if (p.getFechaPedido() == null) return false;
                boolean despuesDe = desde == null || !p.getFechaPedido().isBefore(desde);
                boolean antesDe   = hasta == null || !p.getFechaPedido().isAfter(hasta);
                return despuesDe && antesDe;
            })
            .toList();

        Map<String, Integer> porEstado = filtrados.stream()
            .collect(Collectors.groupingBy(
                p -> p.getEstado() != null ? p.getEstado() : "DESCONOCIDO",
                Collectors.summingInt(p -> 1)));

        Map<String, Integer> porTipo = filtrados.stream()
            .collect(Collectors.groupingBy(
                p -> p.getTipoMadera() != null ? p.getTipoMadera() : "Sin tipo",
                Collectors.summingInt(p -> 1)));

        int totalUnidades = filtrados.stream()
            .mapToInt(p -> p.getCantidadSolicitada() != null ? p.getCantidadSolicitada() : 0)
            .sum();

        guardarHistorial(TipoReporte.PEDIDOS, "sistema",
            System.currentTimeMillis() - inicio, exitoso, "pedidos", fallido);

        return ReportePedidosResponse.builder()
            .desde(desde)
            .hasta(hasta)
            .mina(mina != null ? mina : "TODAS")
            .totalPedidos(filtrados.size())
            .porEstado(porEstado)
            .porTipoMadera(porTipo)
            .totalUnidadesSolicitadas(totalUnidades)
            .detalle(filtrados)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── REPORTE DE ENTREGAS ─────────────────────────────────────────────────
    @Override
    @Transactional
    public ReporteEntregasResponse reporteEntregas(String mina) {
        long inicio = System.currentTimeMillis();
        boolean exitoso = true;
        String fallido = "";
        List<EntregaDto> entregas = new ArrayList<>();

        try {
            entregas = mina != null
                ? entregaClient.porMina(mina)
                : entregaClient.listarTodas();
        } catch (Exception e) {
            exitoso = false;
            fallido = "entrega";
            log.error("Error al obtener entregas desde Feign: {}", e.getMessage());
        }

        List<EntregaDto> completadas = entregas.stream()
            .filter(e -> "ENTREGADO".equals(e.getEstado())).toList();
        List<EntregaDto> enTransito  = entregas.stream()
            .filter(e -> "EN_RUTA".equals(e.getEstado()) || "PREPARANDO".equals(e.getEstado())).toList();
        List<EntregaDto> fallidas    = entregas.stream()
            .filter(e -> "FALLIDO".equals(e.getEstado())).toList();

        double tiempoPromedio = completadas.stream()
            .filter(e -> e.getFechaSalida() != null && e.getFechaEntregaReal() != null)
            // Filtro de seguridad para evitar duraciones negativas
            .filter(e -> e.getFechaEntregaReal().isAfter(e.getFechaSalida()))
            .mapToLong(e -> Duration.between(e.getFechaSalida(), e.getFechaEntregaReal()).toHours())
            .average().orElse(0.0);

        long conConformidad = completadas.stream()
            .filter(e -> Boolean.TRUE.equals(e.getConformidad()))
            .count();
        double pctConformidad = completadas.isEmpty() ? 0.0
            : (double) conConformidad / completadas.size() * 100;

        Map<String, Integer> porMina = entregas.stream()
            .collect(Collectors.groupingBy(
                e -> e.getMinaDestino() != null ? e.getMinaDestino() : "Sin mina",
                Collectors.summingInt(e -> 1)));

        Map<String, Integer> porTransportista = completadas.stream()
            .filter(e -> e.getTransportista() != null)
            .collect(Collectors.groupingBy(
                EntregaDto::getTransportista,
                Collectors.summingInt(e -> 1)));

        guardarHistorial(TipoReporte.ENTREGAS, "sistema",
            System.currentTimeMillis() - inicio, exitoso, "entrega", fallido);

        return ReporteEntregasResponse.builder()
            .totalEntregas(entregas.size())
            .entregasCompletadas(completadas.size())
            .entregasEnTransito(enTransito.size())
            .entregasFallidas(fallidas.size())
            .tiempoPromedioHoras(tiempoPromedio)
            .porcentajeConformidad(pctConformidad)
            .porMina(porMina)
            .porTransportista(porTransportista)
            .detalle(entregas)
            .fechaGeneracion(LocalDateTime.now())
            .build();
    }

    // ─── HELPERS (CORREGIDA LÓGICA DE COLECCIÓN VACÍA) ────────────────────────
    private List<MaderaDto> fetchInventario(List<String> fallidos) {
        try {
            return inventarioClient.listarTodo();
        } catch (Exception e) {
            fallidos.add("inventario");
            log.error("Circuit Breaker/Falla al consultar ms-inventario: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<PedidoDto> fetchPedidos(List<String> fallidos) {
        try {
            return pedidosClient.listarTodos();
        } catch (Exception e) {
            fallidos.add("pedidos");
            log.error("Circuit Breaker/Falla al consultar ms-pedidos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EntregaDto> fetchEntregas(List<String> fallidos) {
        try {
            return entregaClient.listarTodas();
        } catch (Exception e) {
            fallidos.add("entrega");
            log.error("Circuit Breaker/Falla al consultar ms-entrega: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ProveedorDto> fetchProveedores(List<String> fallidos) {
        try {
            return proveedoresClient.listarTodos();
        } catch (Exception e) {
            fallidos.add("proveedores");
            log.error("Circuit Breaker/Falla al consultar ms-proveedores: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<Object> fetchContratosVence(List<String> fallidos) {
        try {
            return proveedoresClient.contratosProximosAVencer();
        } catch (Exception e) {
            log.error("Falla al consultar contratos proximos a vencer: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private void guardarHistorial(TipoReporte tipo, String usuario,
            long tiempo, boolean exitoso,
            String consultados, String fallidos) {
        reporteRepo.save(ReporteGenerado.builder()
            .tipo(tipo)
            .solicitadoPor(usuario)
            .tiempoGeneracionMs(tiempo)
            .exitoso(exitoso)
            .serviciosConsultados(consultados)
            .serviciosFallidos(fallidos)
            .build());
    }

    @Override
    public List<ReporteGenerado> historial() {
        return reporteRepo.findAll();
    }
}
