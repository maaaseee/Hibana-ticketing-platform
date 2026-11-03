package com.aieta.ticketing_app_gateway.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AzureKeyVaultSecretServiceTest {

    @Test
    @DisplayName("Should return default value when Azure Key Vault is disabled")
    void shouldReturnDefaultValueWhenDisabled() {
        AzureKeyVaultSecretService service = new AzureKeyVaultSecretService(false, "");
        service.init();

        String secret = service.getSecret("my-secret", "default-fallback-value");
        assertThat(secret).isEqualTo("default-fallback-value");
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should return default value when endpoint is blank")
    void shouldReturnDefaultValueWhenEndpointBlank() {
        AzureKeyVaultSecretService service = new AzureKeyVaultSecretService(true, "   ");
        service.init();

        String secret = service.getSecret("my-secret", "fallback-123");
        assertThat(secret).isEqualTo("fallback-123");
        assertThat(service.isEnabled()).isFalse();
    }
}
