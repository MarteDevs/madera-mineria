package com.madera.mantenimiento.listener;

import com.madera.mantenimiento.config.RabbitMQConfig;
import com.madera.mantenimiento.dto.EntregaCompletadaEvent;
import com.madera.mantenimiento.service.MantenimientoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EntregaCompletadaListener {

    private final MantenimientoService mantenimientoService;

    @RabbitListener(queues = RabbitMQConfig.COLA_ENTREGAS_COMP)
    public void procesarEntregaCompletada(EntregaCompletadaEvent evento) {
        log.info("Evento de entrega completada recibido: ID Entrega #{} | Placa: {} | Distancia: {} km",
            evento.getEntregaId(),
            evento.getPlacaVehiculo(),
            evento.getDistanciaKm());

        try {
            mantenimientoService.procesarEntregaCompletada(evento);
            log.info("Kilometraje y alertas procesadas con éxito para vehículo: {}",
                evento.getPlacaVehiculo());
        } catch (Exception e) {
            log.error("Error al procesar la entrega completada #{}: {}",
                evento.getEntregaId(), e.getMessage(), e);
            throw e; // Lanza la excepción para que RabbitMQ maneje el reintento / cola de fallos
        }
    }
}
