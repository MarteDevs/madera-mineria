package com.madera.pedidos.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Datos de la madera solicitada
    private Long maderaId;              // referencia al ms-inventario
    private String tipoMadera;          // "eucalipto", "pino", etc.
    private String usoMadera;           // "soporte_galeria", "entibado"

    @Min(1)
    private Integer cantidadSolicitada;

    private String unidad;              // "m3", "metro_lineal", "unidad"

    // Datos de la solicitud
    @NotBlank
    private String mina;                // "Cerro Verde", "Antamina"

    private String areaSolicitante;     // "Operaciones", "Mantenimiento"
    private String solicitadoPor;       // username del comprador

    // Estado y fechas
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    @CreationTimestamp
    private LocalDateTime fechaPedido;

    private LocalDateTime fechaAprobacion;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;

    // Control
    private String aprobadoPor;         // quien aprobó en almacén
    private String motivoRechazo;       // si fue rechazado
    private String observaciones;       // notas adicionales

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
