package com.madera.ms_inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "madera")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Madera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String tipo;              // "eucalipto", "pino", "roble"

    private String uso;               // "soporte_galeria", "entibado", "cuadros"
    private String unidad;            // "m3", "metro_lineal", "unidad"

    @Min(0)
    private Double precioPorUnidad;   // precio en soles

    @Min(0)
    private Integer stockDisponible;

    private Integer stockMinimo;      // alerta cuando baje de este valor
    private String mina;              // "Cerro Verde", "Antamina", "Las Bambas"
    private String proveedor;         // empresa que suministra la madera
    private String estado;            // "DISPONIBLE", "RESERVADO", "AGOTADO"

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
