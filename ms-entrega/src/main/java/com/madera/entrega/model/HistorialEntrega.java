package com.madera.entrega.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "historial_entrega")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistorialEntrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "entrega_id", nullable = false)
    private Entrega entrega;

    @Enumerated(EnumType.STRING)
    private EstadoEntrega estadoAnterior;

    @Enumerated(EnumType.STRING)
    private EstadoEntrega estadoNuevo;

    private String responsable;
    private String motivo;

    @CreationTimestamp
    private LocalDateTime fecha;
}
