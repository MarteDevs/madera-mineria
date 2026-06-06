package com.madera.proveedores.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "proveedor")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String ruc;                    // RUC de la empresa

    @NotBlank
    private String razonSocial;            // nombre legal de la empresa

    private String nombreComercial;        // nombre comercial
    private String contactoNombre;         // nombre del representante
    private String contactoEmail;
    private String contactoTelefono;
    private String direccion;
    private String ciudad;                 // Lima, Arequipa, etc.

    private Boolean activo;

    @CreationTimestamp
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    private LocalDateTime ultimaActualizacion;
}
