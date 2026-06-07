package com.madera.reportes.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reporte_generado")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReporteGenerado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TipoReporte tipo;

    private String solicitadoPor;          // usuario que pidió el reporte
    private String parametros;             // JSON o texto con los filtros usados
    private Long tiempoGeneracionMs;       // milisegundos que tardó

    private Boolean exitoso;               // si todos los servicios respondieron
    private String serviciosConsultados;   // "inventario,pedidos,entrega,proveedores"
    private String serviciosFallidos;      // servicios que no respondieron (caídos)

    @CreationTimestamp
    private LocalDateTime fechaGeneracion;
}
