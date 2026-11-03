package com.aieta.ticketing_app_gateway.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for securely retrieving and managing secrets from Azure Key Vault.
 * Supports production Azure PaaS via Managed Identity (HTTPS) and local zero-cost
 * emulation via Floci-AZ (HTTP).
 */
@Service
public class AzureKeyVaultSecretService {

    private static final Logger log = LoggerFactory.getLogger(AzureKeyVaultSecretService.class);

    private final boolean enabled;
    private final String endpoint;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private SecretClient secretClient;
    private boolean isHttpEmulator = false;

    public AzureKeyVaultSecretService(
            @Value("${azure.keyvault.enabled:false}") boolean enabled,
            @Value("${azure.keyvault.endpoint:}") String endpoint
    ) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @PostConstruct
    public void init() {
        if (!enabled || endpoint == null || endpoint.isBlank()) {
            log.info("[AZURE-KEYVAULT] Key Vault integration is disabled or endpoint is empty.");
            return;
        }

        try {
            log.info("[AZURE-KEYVAULT] Initializing Azure Key Vault integration for [{}]", endpoint);

            if (endpoint.startsWith("https://")) {
                // Production: Official Azure SDK with Managed Identity / DefaultAzureCredential over HTTPS
                this.secretClient = new SecretClientBuilder()
                        .vaultUrl(endpoint)
                        .credential(new DefaultAzureCredentialBuilder().build())
                        .buildClient();
                this.isHttpEmulator = false;
                log.info("[AZURE-KEYVAULT] SecretClient configured with DefaultAzureCredential for [{}]", endpoint);
            } else {
                // Local dev / emulator (Floci-AZ over HTTP)
                this.isHttpEmulator = true;
                log.info("[AZURE-KEYVAULT] Configured HTTP emulator adapter for Floci-AZ Key Vault at [{}]", endpoint);
            }
        } catch (Exception e) {
            log.warn("[AZURE-KEYVAULT] Failed to initialize Key Vault client: {}. Falling back to default properties.", e.getMessage());
            this.secretClient = null;
            this.isHttpEmulator = false;
        }
    }

    /**
     * Resolves a secret from Azure Key Vault or returns the defaultValue if not found/disabled.
     */
    public String getSecret(String secretName, String defaultValue) {
        if (!enabled) {
            return defaultValue;
        }

        if (isHttpEmulator) {
            return getSecretFromEmulator(secretName, defaultValue);
        }

        if (secretClient != null) {
            try {
                KeyVaultSecret secret = secretClient.getSecret(secretName);
                if (secret != null && secret.getValue() != null && !secret.getValue().isBlank()) {
                    log.info("[AZURE-KEYVAULT] Resolved secret [{}] from Azure Key Vault", secretName);
                    return secret.getValue();
                }
            } catch (Exception e) {
                log.debug("[AZURE-KEYVAULT] Secret [{}] not found in Key Vault: {}", secretName, e.getMessage());
            }
        }

        return defaultValue;
    }

    private String getSecretFromEmulator(String secretName, String defaultValue) {
        try {
            String baseUrl = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            URI uri = URI.create(baseUrl + "secrets/" + secretName + "?api-version=7.4");

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer dev-token")
                    .header("Accept", "application/json")
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("value") && !root.get("value").asText().isBlank()) {
                    String val = root.get("value").asText();
                    log.info("[AZURE-KEYVAULT] Resolved secret [{}] from Floci-AZ Key Vault", secretName);
                    return val;
                }
            } else if (response.statusCode() == 404 && defaultValue != null && !defaultValue.isBlank()) {
                // Auto-seed secret into Floci-AZ Key Vault
                seedSecretToEmulator(secretName, defaultValue);
            }
        } catch (Exception e) {
            log.debug("[AZURE-KEYVAULT] Emulator secret lookup failed for [{}]: {}", secretName, e.getMessage());
        }

        return defaultValue;
    }

    private void seedSecretToEmulator(String secretName, String value) {
        try {
            String baseUrl = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            URI uri = URI.create(baseUrl + "secrets/" + secretName + "?api-version=7.4");
            String payload = objectMapper.writeValueAsString(new SecretPayload(value));

            HttpRequest putRequest = HttpRequest.newBuilder(uri)
                    .header("Authorization", "Bearer dev-token")
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(2))
                    .build();

            httpClient.send(putRequest, HttpResponse.BodyHandlers.discarding());
            log.info("[AZURE-KEYVAULT] Auto-seeded secret [{}] into Floci-AZ Key Vault", secretName);
        } catch (Exception e) {
            log.debug("[AZURE-KEYVAULT] Failed to auto-seed secret [{}]: {}", secretName, e.getMessage());
        }
    }

    public boolean isEnabled() {
        return enabled && (secretClient != null || isHttpEmulator);
    }

    private record SecretPayload(String value) {}
}
