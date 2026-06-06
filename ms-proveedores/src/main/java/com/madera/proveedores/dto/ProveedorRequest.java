package com.madera.proveedores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProveedorRequest {

    @NotBlank(message = "El RUC es requerido")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener 11 dígitos")
    private String ruc;

    @NotBlank(message = "La razón social es requerida")
    private String razonSocial;

    private String nombreComercial;
    private String contactoNombre;
    private String contactoEmail;
    private String contactoTelefono;
    private String direccion;
    private String ciudad;
}
