package com.madera.mantenimiento.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehiculo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String placa;                  // Placa del vehículo (ej: ABC-123)

    private String marca;                  // Volvo, Scania, Mercedes
    private String modelo;
    private Integer anio;
    private String tipo;                   // "camion", "camioneta", "furgon"
    @Column(name = "capacidad_toneladas_m3")
    private Double capacidadToneladasM3;   // Capacidad de carga en m3 o toneladas

    // Conductor asignado
    private String conductorNombre;
    private String conductorLicencia;
    private String conductorTelefono;

    // Kilometraje
    private Double kmActual;               // Kilometraje actual
    private Double kmUltimoMantenimiento;  // Kilometraje en el último mantenimiento
    private Double kmProximoMantenimiento; // Kilometraje programado para próximo servicio

    // Fechas
    private LocalDate fechaUltimoMantenimiento;
    private LocalDate fechaProximoMantenimiento;
    private LocalDate vencimientoSoat;
    private LocalDate vencimientoRevisionTecnica;

    @Enumerated(EnumType.STRING)
    private EstadoVehiculo estado;

    private Integer totalEntregasRealizadas;
    private Boolean requiereMantenimiento;  // Alerta activa de mantenimiento

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
