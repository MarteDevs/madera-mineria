package com.madera.proveedores.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrega_proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @ManyToOne
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    private Long maderaId;                 // ID en ms-inventario
    private String tipoMadera;
    private Integer cantidadEntregada;
    private String unidad;
    private Double precioUnitario;
    private Double montoTotal;             // calculado automáticamente

    private String guiaRemision;           // número de guía de remisión
    private String responsableRecepcion;   // quien recibió la madera

    private Boolean stockActualizado;      // si Feign fue exitoso

    @CreationTimestamp
    private LocalDateTime fechaEntrega;
}
