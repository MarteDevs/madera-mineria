package com.madera.proveedores.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Reutiliza el exchange existente del sistema
    public static final String EXCHANGE         = "madera.exchange";
    public static final String COLA_PROVEEDORES = "cola.entregas.proveedor";
    public static final String ROUTING_PROVEEDOR= "entrega.proveedor";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaEntregasProveedor() {
        return new Queue(COLA_PROVEEDORES, true);
    }

    @Bean
    public Binding bindingProveedor(
            Queue colaEntregasProveedor,
            TopicExchange maderaExchange) {
        return BindingBuilder
            .bind(colaEntregasProveedor)
            .to(maderaExchange)
            .with(ROUTING_PROVEEDOR);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
