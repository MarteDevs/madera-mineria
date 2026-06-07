package com.madera.reportes.client;

import com.madera.reportes.dto.externos.ProveedorDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;

@FeignClient(
    name = "ms-proveedores",
    fallback = ProveedoresClientFallback.class
)
public interface ProveedoresClient {

    @GetMapping("/api/proveedores")
    List<ProveedorDto> listarTodos();

    @GetMapping("/api/proveedores/activos")
    List<ProveedorDto> listarActivos();

    @GetMapping("/api/proveedores/contratos/proximos-a-vencer")
    List<Object> contratosProximosAVencer();
}
