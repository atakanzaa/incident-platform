package com.incident.dashboard_service.service;

import com.incident.dashboard_service.model.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertConsumer {

    private final DashboardService dashboardService;

    @KafkaListener(
            topics = {"${kafka.topics.alerts-critical}", "${kafka.topics.alerts-info}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAlert(
            @Payload Alert alert,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.debug("Consumed alert from topic: {}, partition: {}, offset: {}, alertId: {}", 
            topic, partition, offset, alert.getAlertId());
        
        try {
            // Process alert for dashboard
            dashboardService.processNewAlert(alert);
            
            log.debug("Successfully processed alert: {} for dashboard", alert.getAlertId());
            
        } catch (Exception e) {
            log.error("Error processing alert: {} for dashboard", alert.getAlertId(), e);
            // Don't rethrow - dashboard is for display only, shouldn't block processing
        }
    }
} 