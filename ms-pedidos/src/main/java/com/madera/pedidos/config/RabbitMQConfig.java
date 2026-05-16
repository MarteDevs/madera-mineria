package com.madera.pedidos.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE    = "madera.exchange";
    public static final String QUEUE       = "cola.pedidos.creados";
    public static final String ROUTING_KEY = "pedido.creado";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaPedidosCreados() {
        // durable=true: la cola sobrevive si RabbitMQ se reinicia
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding binding(Queue cola, TopicExchange exchange) {
        return BindingBuilder
            .bind(cola)
            .to(exchange)
            .with(ROUTING_KEY);
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
