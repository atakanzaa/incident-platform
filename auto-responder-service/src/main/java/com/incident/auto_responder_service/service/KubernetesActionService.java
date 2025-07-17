package com.incident.auto_responder_service.service;

import com.incident.auto_responder_service.model.ActionResult;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KubernetesActionService {

    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;

    @Value("${kubernetes.config.namespace}")
    private String namespace;

    @Value("${auto-response.dry-run}")
    private boolean dryRun;

    public ActionResult restartPods(String serviceName, String alertId) {
        String actionId = UUID.randomUUID().toString();
        ActionResult result = ActionResult.builder()
                .actionId(actionId)
                .alertId(alertId)
                .serviceName(serviceName)
                .actionType(ActionResult.ActionType.POD_RESTART)
                .status(ActionResult.ActionStatus.EXECUTING)
                .description("Restarting pods for service: " + serviceName)
                .executedAt(LocalDateTime.now())
                .dryRun(dryRun)
                .build();

        try {
            if (dryRun) {
                log.info("[DRY RUN] Would restart pods for service: {}", serviceName);
                result.setStatus(ActionResult.ActionStatus.DRY_RUN);
                result.setSuccess(true);
                result.setDescription("DRY RUN: Would restart pods for service: " + serviceName);
                return result;
            }

            // Find deployment by service name (assuming deployment name matches service name)
            V1Deployment deployment = findDeployment(serviceName);
            if (deployment == null) {
                result.setStatus(ActionResult.ActionStatus.FAILED);
                result.setSuccess(false);
                result.setErrorMessage("Deployment not found for service: " + serviceName);
                return result;
            }

            // Restart by updating deployment annotation to trigger rollout
            restartDeployment(deployment);

            result.setStatus(ActionResult.ActionStatus.COMPLETED);
            result.setSuccess(true);
            result.setCompletedAt(LocalDateTime.now());
            result.setDescription("Successfully triggered pod restart for service: " + serviceName);

            log.info("Pod restart completed for service: {}", serviceName);

        } catch (Exception e) {
            log.error("Failed to restart pods for service: {}", serviceName, e);
            result.setStatus(ActionResult.ActionStatus.FAILED);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(LocalDateTime.now());
        }

        return result;
    }

    public ActionResult scaleService(String serviceName, String alertId, int targetReplicas) {
        String actionId = UUID.randomUUID().toString();
        ActionResult result = ActionResult.builder()
                .actionId(actionId)
                .alertId(alertId)
                .serviceName(serviceName)
                .actionType(ActionResult.ActionType.SERVICE_SCALE_UP)
                .status(ActionResult.ActionStatus.EXECUTING)
                .description("Scaling service: " + serviceName + " to " + targetReplicas + " replicas")
                .executedAt(LocalDateTime.now())
                .dryRun(dryRun)
                .build();

        try {
            if (dryRun) {
                log.info("[DRY RUN] Would scale service: {} to {} replicas", serviceName, targetReplicas);
                result.setStatus(ActionResult.ActionStatus.DRY_RUN);
                result.setSuccess(true);
                result.setDescription("DRY RUN: Would scale service: " + serviceName + " to " + targetReplicas + " replicas");
                return result;
            }

            V1Deployment deployment = findDeployment(serviceName);
            if (deployment == null) {
                result.setStatus(ActionResult.ActionStatus.FAILED);
                result.setSuccess(false);
                result.setErrorMessage("Deployment not found for service: " + serviceName);
                return result;
            }

            // Scale the deployment
            scaleDeployment(deployment, targetReplicas);

            Map<String, Object> details = new HashMap<>();
            details.put("targetReplicas", targetReplicas);
            details.put("previousReplicas", deployment.getSpec().getReplicas());
            result.setDetails(details);

            result.setStatus(ActionResult.ActionStatus.COMPLETED);
            result.setSuccess(true);
            result.setCompletedAt(LocalDateTime.now());
            result.setDescription("Successfully scaled service: " + serviceName + " to " + targetReplicas + " replicas");

            log.info("Service scaling completed for service: {} to {} replicas", serviceName, targetReplicas);

        } catch (Exception e) {
            log.error("Failed to scale service: {} to {} replicas", serviceName, targetReplicas, e);
            result.setStatus(ActionResult.ActionStatus.FAILED);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(LocalDateTime.now());
        }

        return result;
    }

    public ActionResult checkServiceHealth(String serviceName, String alertId) {
        String actionId = UUID.randomUUID().toString();
        ActionResult result = ActionResult.builder()
                .actionId(actionId)
                .alertId(alertId)
                .serviceName(serviceName)
                .actionType(ActionResult.ActionType.HEALTH_CHECK)
                .status(ActionResult.ActionStatus.EXECUTING)
                .description("Checking health for service: " + serviceName)
                .executedAt(LocalDateTime.now())
                .dryRun(false)
                .build();

        try {
            V1Deployment deployment = findDeployment(serviceName);
            if (deployment == null) {
                result.setStatus(ActionResult.ActionStatus.FAILED);
                result.setSuccess(false);
                result.setErrorMessage("Deployment not found for service: " + serviceName);
                return result;
            }

            V1DeploymentStatus status = deployment.getStatus();
            Map<String, Object> details = new HashMap<>();
            details.put("replicas", status.getReplicas());
            details.put("readyReplicas", status.getReadyReplicas());
            details.put("availableReplicas", status.getAvailableReplicas());
            details.put("unavailableReplicas", status.getUnavailableReplicas());

            result.setDetails(details);
            result.setStatus(ActionResult.ActionStatus.COMPLETED);
            result.setSuccess(true);
            result.setCompletedAt(LocalDateTime.now());
            result.setDescription("Health check completed for service: " + serviceName);

            log.debug("Health check completed for service: {}", serviceName);

        } catch (Exception e) {
            log.error("Failed to check health for service: {}", serviceName, e);
            result.setStatus(ActionResult.ActionStatus.FAILED);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setCompletedAt(LocalDateTime.now());
        }

        return result;
    }

    private V1Deployment findDeployment(String serviceName) throws ApiException {
        try {
            V1DeploymentList deployments = appsV1Api.listNamespacedDeployment(namespace).execute();
            
            // Filter by service name
            for (V1Deployment deployment : deployments.getItems()) {
                if (serviceName.equals(deployment.getMetadata().getName())) {
                    return deployment;
                }
            }
            
            log.warn("No deployment found for service: {}", serviceName);
            return null;
        } catch (ApiException e) {
            log.error("Error finding deployment for service: {}", serviceName, e);
            throw e;
        }
    }

    private void restartDeployment(V1Deployment deployment) throws ApiException {
        String deploymentName = deployment.getMetadata().getName();
        
        // Add annotation to trigger rollout restart
        Map<String, String> annotations = deployment.getSpec().getTemplate().getMetadata().getAnnotations();
        if (annotations == null) {
            annotations = new HashMap<>();
        }
        annotations.put("kubectl.kubernetes.io/restartedAt", OffsetDateTime.now().toString());
        
        deployment.getSpec().getTemplate().getMetadata().setAnnotations(annotations);
        
        appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
    }

    private void scaleDeployment(V1Deployment deployment, int targetReplicas) throws ApiException {
        String deploymentName = deployment.getMetadata().getName();
        
        deployment.getSpec().setReplicas(targetReplicas);
        
        appsV1Api.replaceNamespacedDeployment(deploymentName, namespace, deployment).execute();
    }
} 