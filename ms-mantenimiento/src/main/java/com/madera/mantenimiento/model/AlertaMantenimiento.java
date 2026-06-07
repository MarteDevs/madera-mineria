package com.madera.mantenimiento.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alerta_mantenimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    private String tipoAlerta;             // "KM_EXCEDIDO", "SOAT_POR_VENCER", "REVISION_POR_VENCER"
    private String mensaje;
    private String prioridad;              // "ALTA", "MEDIA", "BAJA"
    private Boolean resuelta;

    @CreationTimestamp
    private LocalDateTime fechaGeneracion;

    private LocalDateTime fechaResolucion;
}
