package com.madera.mantenimiento.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange compartido del sistema
    public static final String EXCHANGE              = "madera.exchange";

    // Cola exclusiva para ms-mantenimiento
    public static final String COLA_ENTREGAS_COMP   = "cola.entregas.completadas";
    public static final String ROUTING_ENTREGA_COMP = "entrega.completada";

    @Bean
    public TopicExchange maderaExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue colaEntregasCompletadas() {
        return new Queue(COLA_ENTREGAS_COMP, true);
    }

    @Bean
    public Binding bindingEntregaCompletada(
            Queue colaEntregasCompletadas,
            TopicExchange maderaExchange) {
        return BindingBuilder
            .bind(colaEntregasCompletadas)
            .to(maderaExchange)
            .with(ROUTING_ENTREGA_COMP);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
            new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
