package com.madera.mantenimiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehiculoResponse {
    private Long id;
    private String placa;
    private String marca;
    private String modelo;
    private String tipo;
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;
    private Double kmActual;
    private Double kmProximoMantenimiento;
    private Double kmRestantesParaMantenimiento;
    private LocalDate fechaUltimoMantenimiento;
    private LocalDate fechaProximoMantenimiento;
    private LocalDate vencimientoSoat;
    private LocalDate vencimientoRevisionTecnica;
    private String estado;
    private Boolean requiereMantenimiento;
    private Integer totalEntregasRealizadas;
}
