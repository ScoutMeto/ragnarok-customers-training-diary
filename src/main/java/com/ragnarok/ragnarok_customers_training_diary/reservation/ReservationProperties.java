package com.ragnarok.ragnarok_customers_training_diary.reservation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Konfigurace pro integraci s externím rezervačním systémem
 * (https://github.com/ScoutMeto/ragnarok_customers_reservation_system).
 *
 * <p>Mapuje properties {@code ragnarok.reservation.*}.
 */
@Configuration
@ConfigurationProperties(prefix = "ragnarok.reservation")
public class ReservationProperties {

    /** Base URL rezervačního systému (bez koncového lomítka). */
    private String baseUrl = "http://localhost:8081";

    /** Timeout pro HTTP volání v sekundách. */
    private int timeoutSeconds = 8;

    /** Když {@code false}, klient nikdy nevolá rezervační API (užitečné v testech). */
    private boolean enabled = true;

    /** Phase 7.2: sdílený klíč pro server-to-server cancel (hlavička X-Api-Key). */
    private String apiKey = "";

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) {
        // strip trailing slash
        if (baseUrl != null && baseUrl.endsWith("/")) {
            this.baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        } else {
            this.baseUrl = baseUrl;
        }
    }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
