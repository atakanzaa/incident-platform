package com.incident.log_collector_service.service;

import com.incident.log_collector_service.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProducer {

    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    @Value("${kafka.topics.logs-raw}")
    private String logsRawTopic;

    public CompletableFuture<SendResult<String, LogEvent>> sendLog(LogEvent logEvent) {
        // Generate ID if not present
        if (logEvent.getId() == null) {
            logEvent.setId(UUID.randomUUID().toString());
        }

        // Create message with headers
        Message<LogEvent> message = MessageBuilder
                .withPayload(logEvent)
                .setHeader(KafkaHeaders.TOPIC, logsRawTopic)
                .setHeader(KafkaHeaders.KEY, logEvent.getServiceName())
                .setHeader("service", logEvent.getServiceName())
                .setHeader("level", logEvent.getLevel().toString())
                .build();

        // Send message
        CompletableFuture<SendResult<String, LogEvent>> future = kafkaTemplate.send(message);

        // Add callbacks
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Sent log event: {} to topic: {} with key: {}", 
                    logEvent.getId(), 
                    result.getRecordMetadata().topic(),
                    result.getProducerRecord().key());
            } else {
                log.error("Failed to send log event: {}", logEvent.getId(), ex);
            }
        });

        return future;
    }

    public void sendLogSync(LogEvent logEvent) {
        try {
            sendLog(logEvent).get();
        } catch (Exception e) {
            log.error("Error sending log sync: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send log event", e);
        }
    }
} 