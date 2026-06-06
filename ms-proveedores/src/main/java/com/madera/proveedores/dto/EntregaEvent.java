package com.madera.proveedores.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntregaEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long entregaId;
    private String proveedorNombre;
    private String tipoMadera;
    private Integer cantidadEntregada;
    private LocalDateTime fechaEntrega;
}
