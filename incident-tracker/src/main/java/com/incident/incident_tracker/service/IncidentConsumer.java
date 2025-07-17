package com.incident.incident_tracker.service;

import com.incident.incident_tracker.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentConsumer {

    private final IncidentService incidentService;

    @KafkaListener(
            topics = {"${kafka.topics.alerts-critical}", "${kafka.topics.alerts-info}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAlert(
            @Payload Alert alert,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.debug("Consumed alert from topic: {}, partition: {}, offset: {}, alertId: {}", 
            topic, partition, offset, alert.getAlertId());
        
        try {
            // Store alert as incident
            incidentService.createIncidentFromAlert(alert);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed alert: {} from topic: {}", alert.getAlertId(), topic);
            
        } catch (Exception e) {
            log.error("Error processing alert: {} from topic: {}", alert.getAlertId(), topic, e);
            // Acknowledge anyway to avoid blocking - incidents are for historical storage
            acknowledgment.acknowledge();
        }
    }
} 