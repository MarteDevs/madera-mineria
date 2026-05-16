package com.madera.entrega.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long pedidoId;
    private String tipoMadera;
    private Integer cantidad;
    private String unidad;

    private String almacenOrigen;
    private String minaDestino;
    private String direccionMina;
    private Double distanciaKm;

    private String transportista;
    private String vehiculo;
    private String telefTransportista;

    @Enumerated(EnumType.STRING)
    private EstadoEntrega estado;

    @CreationTimestamp
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaSalida;
    private LocalDateTime fechaEntregaEstimada;
    private LocalDateTime fechaEntregaReal;

    private String recibidoPor;
    private String observaciones;
    private Boolean conformidad;

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
