package com.madera.notificaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.madera.notificaciones.dto.NotificacionResponse;
import com.madera.notificaciones.dto.PedidoEvent;
import com.madera.notificaciones.model.EstadoNotificacion;
import com.madera.notificaciones.model.Notificacion;
import com.madera.notificaciones.repository.NotificacionRepository;
import com.madera.notificaciones.service.NotificacionServiceImpl;

@ExtendWith(MockitoExtension.class)
class MsNotificacionesApplicationTests {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    @Test
    void procesarEventoGuardaNotificacionPendiente() {
        PedidoEvent evento = new PedidoEvent(
                1L,
                "eucalipto",
                5,
                "Cerro Verde",
                "Operaciones",
                "CREADO",
                null);

        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        notificacionService.procesarEvento(evento);

        ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository).save(captor.capture());

        Notificacion guardada = captor.getValue();
        assertEquals(1L, guardada.getPedidoId());
        assertEquals("eucalipto", guardada.getTipoMadera());
        assertEquals(5, guardada.getCantidad());
        assertEquals("Cerro Verde", guardada.getMina());
        assertEquals("Operaciones", guardada.getAreaSolicitante());
        assertEquals("CREADO", guardada.getTipoEvento());
        assertEquals(EstadoNotificacion.PENDIENTE, guardada.getEstado());
        assertEquals("ALMACEN", guardada.getDestinatario());
        assertEquals(
                "Nuevo pedido #1: 5 unidades de eucalipto solicitadas por la mina Cerro Verde. Requiere aprobación.",
                guardada.getMensaje());
        assertNull(guardada.getFechaLeida());
    }

    @Test
    void marcarLeidaActualizaEstadoYFecha() {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(10L);
        notificacion.setPedidoId(44L);
        notificacion.setTipoMadera("pino");
        notificacion.setCantidad(3);
        notificacion.setMina("Antamina");
        notificacion.setTipoEvento("APROBADO");
        notificacion.setMensaje("Mensaje");
        notificacion.setEstado(EstadoNotificacion.PENDIENTE);
        notificacion.setDestinatario("ALMACEN");

        when(notificacionRepository.findById(10L)).thenReturn(Optional.of(notificacion));
        when(notificacionRepository.save(any(Notificacion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificacionResponse response = notificacionService.marcarLeida(10L);

        assertEquals("LEIDA", response.getEstado());
        assertNotNull(response.getFechaLeida());
        assertEquals(10L, response.getId());
        verify(notificacionRepository).save(notificacion);
    }
}
