package com.madera.proveedores.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contrato")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    private String tipoMadera;             // "eucalipto", "pino", "roble"
    private String unidad;                 // "m3", "metro_lineal"
    private Double precioPactado;          // precio por unidad en soles
    private Integer cantidadMinima;        // cantidad mínima por entrega
    private Integer cantidadMaxima;        // cantidad máxima por entrega

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    private EstadoContrato estado;

    private String observaciones;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;
}
