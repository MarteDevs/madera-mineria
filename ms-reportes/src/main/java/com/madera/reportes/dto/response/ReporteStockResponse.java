package com.madera.reportes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteStockResponse {
    private String mina;
    private List<ItemStockDto> items;
    private Integer totalUnidades;
    private Integer itemsConStockBajo;
    private LocalDateTime fechaGeneracion;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ItemStockDto {
        private String tipo;
        private String uso;
        private Integer stock;
        private Integer stockMinimo;
        private Boolean stockBajo;
        private String unidad;
        private Double precioPorUnidad;
    }
}
