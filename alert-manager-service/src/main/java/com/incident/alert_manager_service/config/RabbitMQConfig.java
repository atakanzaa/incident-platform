package com.incident.alert_manager_service.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {

    @Value("${rabbitmq.exchanges.alert-exchange}")
    private String alertExchange;

    @Value("${rabbitmq.queues.notifications}")
    private String notificationsQueue;

    @Value("${rabbitmq.queues.auto-actions}")
    private String autoActionsQueue;

    @Value("${rabbitmq.queues.critical-alerts}")
    private String criticalAlertsQueue;

    @Value("${rabbitmq.routing-keys.notification}")
    private String notificationRoutingKey;

    @Value("${rabbitmq.routing-keys.auto-action}")
    private String autoActionRoutingKey;

    @Value("${rabbitmq.routing-keys.critical}")
    private String criticalRoutingKey;

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setMandatory(true);
        return template;
    }

    // Exchange
    @Bean
    public TopicExchange alertExchange() {
        return ExchangeBuilder.topicExchange(alertExchange)
                .durable(true)
                .build();
    }

    // Queues
    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(notificationsQueue)
                .withArgument("x-message-ttl", 3600000) // 1 hour TTL
                .build();
    }

    @Bean
    public Queue autoActionsQueue() {
        return QueueBuilder.durable(autoActionsQueue)
                .withArgument("x-message-ttl", 1800000) // 30 minutes TTL
                .build();
    }

    @Bean
    public Queue criticalAlertsQueue() {
        return QueueBuilder.durable(criticalAlertsQueue)
                .withArgument("x-message-ttl", 7200000) // 2 hours TTL
                .build();
    }

    // Bindings
    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationsQueue())
                .to(alertExchange())
                .with(notificationRoutingKey);
    }

    @Bean
    public Binding autoActionBinding() {
        return BindingBuilder.bind(autoActionsQueue())
                .to(alertExchange())
                .with(autoActionRoutingKey);
    }

    @Bean
    public Binding criticalAlertBinding() {
        return BindingBuilder.bind(criticalAlertsQueue())
                .to(alertExchange())
                .with(criticalRoutingKey);
    }

    // Dead Letter Queue setup for failed messages
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("alerts.dlq").build();
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return ExchangeBuilder.topicExchange("alerts.dlx").durable(true).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dlq");
    }
} 