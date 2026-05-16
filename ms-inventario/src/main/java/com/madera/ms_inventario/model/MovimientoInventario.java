package com.madera.ms_inventario.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movimiento_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "madera_id")
    private Madera madera;            // qué madera se movió

    @Enumerated(EnumType.STRING)
    private TipoMovimiento tipo;      // ENTRADA o SALIDA

    private Integer cantidad;         // unidades que entraron o salieron
    private String motivo;            // "Pedido #23", "Compra a proveedor"
    private String responsable;       // usuario que registró el movimiento

    @CreationTimestamp
    private LocalDateTime fecha;
}
