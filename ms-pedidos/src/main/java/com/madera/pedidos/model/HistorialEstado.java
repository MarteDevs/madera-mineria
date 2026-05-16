package com.madera.pedidos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_estado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoPedido estadoNuevo;

    private String responsable;         // quien hizo el cambio
    private String motivo;              // razón del cambio

    @CreationTimestamp
    private LocalDateTime fecha;
}
