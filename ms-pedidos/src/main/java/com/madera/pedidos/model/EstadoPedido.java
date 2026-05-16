package com.madera.pedidos.model;

public enum EstadoPedido {
    PENDIENTE,        // recién creado, esperando aprobación
    APROBADO,         // almacén aprobó, se descuenta stock
    EN_PREPARACION,   // el almacén está alistando la madera
    DESPACHADO,       // salió hacia la mina
    ENTREGADO,        // confirmación de recepción en la mina
    RECHAZADO         // sin stock o rechazado por el almacén
}
