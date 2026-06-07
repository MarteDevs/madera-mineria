package com.madera.reportes.dto.externos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProveedorDto {
    private Long id;
    private String ruc;
    private String razonSocial;
    private String ciudad;
    private Boolean activo;
}
