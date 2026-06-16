package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the WSEP external systems integration. Bound to the
 * {@code wsep.*} prefix in {@code application.properties} so the endpoint
 * and timeouts can be changed without recompiling — V3 spec requires that
 * external-system endpoints are configurable, not hard-coded.
 *
 * <p>Defaults match the WSEP 2026 spec.
 */
@ConfigurationProperties(prefix = "wsep")
public class WsepProperties {

    /** Base URL of the WSEP service. */
    private String baseUrl = "https://damp-lynna-wsep-1984852e.koyeb.app/";

    /** TCP connect timeout, in milliseconds. */
    private int connectTimeoutMs = 3_000;

    /** Read timeout (how long to wait for the response), in milliseconds. */
    private int readTimeoutMs = 10_000;

    /**
     * When {@code true}, the application wires the WSEP gateway implementations
     * instead of the in-memory stubs. Off by default so local dev and tests
     * keep working without hitting the real service.
     */
    private boolean enabled = false;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public void setReadTimeoutMs(int readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
