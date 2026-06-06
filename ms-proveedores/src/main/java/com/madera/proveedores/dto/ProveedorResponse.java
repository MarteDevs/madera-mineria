package com.madera.proveedores.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProveedorResponse {
    private Long id;
    private String ruc;
    private String razonSocial;
    private String nombreComercial;
    private String contactoNombre;
    private String contactoEmail;
    private String contactoTelefono;
    private String direccion;
    private String ciudad;
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
