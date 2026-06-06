package com.madera.proveedores.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EntregaProveedorResponse {
    private Long id;
    private String proveedorNombre;
    private String tipoMadera;
    private Integer cantidadEntregada;
    private String unidad;
    private Double precioUnitario;
    private Double montoTotal;
    private String guiaRemision;
    private Boolean stockActualizado;
    private LocalDateTime fechaEntrega;
}
