package com.madera.reportes.dto.externos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaderaDto {
    private Long id;
    private String tipo;
    private String uso;
    private String unidad;
    private Double precioPorUnidad;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private String mina;
    private String estado;
}
