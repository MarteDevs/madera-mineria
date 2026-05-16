package com.madera.ms_inventario.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MaderaResponse {
    private Long id;
    private String tipo;
    private String uso;
    private String unidad;
    private Double precioPorUnidad;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private String mina;
    private String proveedor;
    private String estado;
    private LocalDateTime ultimaActualizacion;
}
