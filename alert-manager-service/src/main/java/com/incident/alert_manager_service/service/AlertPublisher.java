package com.incident.alert_manager_service.service;

import com.incident.alert_manager_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisher {

    private final KafkaTemplate<String, Alert> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${kafka.topics.alerts-critical}")
    private String alertsCriticalTopic;

    @Value("${kafka.topics.alerts-info}")
    private String alertsInfoTopic;

    @Value("${rabbitmq.exchanges.alert-exchange}")
    private String alertExchange;

    @Value("${rabbitmq.routing-keys.notification}")
    private String notificationRoutingKey;

    @Value("${rabbitmq.routing-keys.auto-action}")
    private String autoActionRoutingKey;

    @Value("${rabbitmq.routing-keys.critical}")
    private String criticalRoutingKey;

    public void publishAlert(Alert alert) {
        try {
            // Publish to Kafka for dashboard consumption
            publishToKafka(alert);
            
            // Publish to RabbitMQ based on severity
            publishToRabbitMQ(alert);
            
            log.info("Alert published successfully: {} with severity: {}", 
                alert.getAlertId(), alert.getSeverity());
                
        } catch (Exception e) {
            log.error("Error publishing alert: {}", alert.getAlertId(), e);
            throw new RuntimeException("Failed to publish alert", e);
        }
    }

    private void publishToKafka(Alert alert) {
        String topic = alert.getSeverity() == Alert.AlertSeverity.CRITICAL || 
                      alert.getSeverity() == Alert.AlertSeverity.HIGH ? 
                      alertsCriticalTopic : alertsInfoTopic;

        kafkaTemplate.send(topic, alert.getServiceName(), alert)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.debug("Alert sent to Kafka topic: {} with key: {}", 
                        topic, alert.getServiceName());
                } else {
                    log.error("Failed to send alert to Kafka: {}", alert.getAlertId(), ex);
                }
            });
    }

    private void publishToRabbitMQ(Alert alert) {
        // Send to notifications queue for all alerts
        rabbitTemplate.convertAndSend(
            alertExchange, 
            notificationRoutingKey, 
            alert,
            message -> {
                message.getMessageProperties().setHeader("alertId", alert.getAlertId());
                message.getMessageProperties().setHeader("severity", alert.getSeverity().toString());
                message.getMessageProperties().setHeader("serviceName", alert.getServiceName());
                return message;
            }
        );

        // Send to auto-actions queue for critical and high severity alerts
        if (alert.getSeverity() == Alert.AlertSeverity.CRITICAL || 
            alert.getSeverity() == Alert.AlertSeverity.HIGH) {
            
            rabbitTemplate.convertAndSend(
                alertExchange, 
                autoActionRoutingKey, 
                alert,
                message -> {
                    message.getMessageProperties().setHeader("alertId", alert.getAlertId());
                    message.getMessageProperties().setHeader("severity", alert.getSeverity().toString());
                    message.getMessageProperties().setHeader("serviceName", alert.getServiceName());
                    message.getMessageProperties().setHeader("actionRequired", true);
                    return message;
                }
            );
        }

        // Send to critical alerts queue for critical alerts only
        if (alert.getSeverity() == Alert.AlertSeverity.CRITICAL) {
            rabbitTemplate.convertAndSend(
                alertExchange, 
                criticalRoutingKey, 
                alert,
                message -> {
                    message.getMessageProperties().setHeader("alertId", alert.getAlertId());
                    message.getMessageProperties().setHeader("severity", alert.getSeverity().toString());
                    message.getMessageProperties().setHeader("serviceName", alert.getServiceName());
                    message.getMessageProperties().setHeader("urgent", true);
                    return message;
                }
            );
        }

        log.debug("Alert sent to RabbitMQ for alert: {} with severity: {}", 
            alert.getAlertId(), alert.getSeverity());
    }

    public void publishTestAlert() {
        Alert testAlert = Alert.builder()
            .alertId("TEST-ALERT")
            .serviceName("test-service")
            .severity(Alert.AlertSeverity.INFO)
            .status(Alert.AlertStatus.OPEN)
            .title("Test Alert")
            .description("This is a test alert")
            .anomalyScore(0.5)
            .build();
            
        publishAlert(testAlert);
    }
} 