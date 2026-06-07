package com.madera.mantenimiento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "registro_mantenimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Enumerated(EnumType.STRING)
    private TipoMantenimiento tipo;

    private String descripcion;            // Qué se hizo
    private String taller;                 // Dónde se hizo
    private Double kmAlServicio;           // Kilometraje cuando entró al taller
    private Double costo;                  // Costo en soles / moneda local

    private String tecnicoResponsable;
    private String observaciones;

    private LocalDate fechaIngreso;
    private LocalDate fechaSalida;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;
}
