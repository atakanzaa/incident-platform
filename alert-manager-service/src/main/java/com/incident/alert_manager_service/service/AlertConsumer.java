package com.incident.alert_manager_service.service;

import com.incident.alert_manager_service.model.ScoredLogEvent;
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
public class AlertConsumer {

    private final AlertService alertService;

    @KafkaListener(
            topics = "${kafka.topics.logs-scored}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeScoredLogEvent(
            @Payload ScoredLogEvent scoredLogEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.debug("Consumed scored log event from topic: {}, partition: {}, offset: {}", 
            topic, partition, offset);
        
        try {
            // Only process events with anomaly scores above a minimum threshold
            if (scoredLogEvent.getAnomalyScore() > 0.1) {
                alertService.processAlert(scoredLogEvent);
            } else {
                log.debug("Skipping low-score event: {} with score: {}", 
                    scoredLogEvent.getId(), scoredLogEvent.getAnomalyScore());
            }
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing scored log event: {}", scoredLogEvent.getId(), e);
            // Acknowledge anyway to avoid blocking - could implement retry logic or DLQ here
            acknowledgment.acknowledge();
        }
    }
} 