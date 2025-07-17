package com.incident.auto_responder_service.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Slf4j
public class KubernetesConfig {

    @Value("${kubernetes.config.in-cluster}")
    private boolean inCluster;

    @Value("${kubernetes.config.config-path}")
    private String configPath;

    @Value("${kubernetes.client.timeout}")
    private int timeout;

    @Value("${kubernetes.client.connection-timeout}")
    private int connectionTimeout;

    @Bean
    public ApiClient kubernetesApiClient() throws IOException {
        ApiClient client;
        
        if (inCluster) {
            log.info("Configuring Kubernetes client for in-cluster mode");
            client = Config.fromCluster();
        } else {
            log.info("Configuring Kubernetes client for external mode with config: {}", configPath);
            client = Config.fromConfig(configPath);
        }
        
        client.setConnectTimeout(connectionTimeout);
        client.setReadTimeout(timeout);
        client.setWriteTimeout(timeout);
        
        // Set as default client
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        
        log.info("Kubernetes API client configured successfully");
        return client;
    }

    @Bean
    public CoreV1Api coreV1Api(ApiClient apiClient) {
        return new CoreV1Api(apiClient);
    }

    @Bean
    public AppsV1Api appsV1Api(ApiClient apiClient) {
        return new AppsV1Api(apiClient);
    }
} 