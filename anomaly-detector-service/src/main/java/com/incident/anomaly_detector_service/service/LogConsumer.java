package com.incident.anomaly_detector_service.service;

import com.incident.anomaly_detector_service.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogConsumer {

    private final AnomalyDetectionService anomalyDetectionService;

    @KafkaListener(
            topics = "${kafka.topics.logs-raw}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLog(
            @Payload LogEvent logEvent,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        log.debug("Consumed log from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        
        try {
            // Process the log event for anomaly detection
            anomalyDetectionService.processLogEvent(logEvent);
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing log event: {}", logEvent.getId(), e);
            // Could implement retry logic or send to DLQ here
            acknowledgment.acknowledge(); // Still acknowledge to avoid blocking
        }
    }

    @KafkaListener(
            topics = "${kafka.topics.logs-raw}",
            groupId = "${spring.kafka.consumer.group-id}-batch",
            containerFactory = "batchKafkaListenerContainerFactory",
            batch = "true"
    )
    public void consumeLogsBatch(
            @Payload List<LogEvent> logEvents,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment
    ) {
        log.debug("Consumed batch of {} logs from topic: {}", logEvents.size(), topic);
        
        try {
            // Process batch of log events
            anomalyDetectionService.processLogEventsBatch(logEvents);
            
            // Acknowledge all messages in the batch
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing batch of {} log events", logEvents.size(), e);
            acknowledgment.acknowledge(); // Still acknowledge to avoid blocking
        }
    }
} 