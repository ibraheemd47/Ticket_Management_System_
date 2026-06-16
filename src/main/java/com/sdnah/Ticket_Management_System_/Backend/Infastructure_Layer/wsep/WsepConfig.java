package com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;

/**
 * Spring wiring for the WSEP external systems integration.
 *
 * <p>The whole configuration is gated behind {@code wsep.enabled=true}. When
 * the flag is off (the default), this file produces no beans and Spring
 * autowires the existing in-memory stubs ({@code PaymentGatewayProxy} /
 * {@code TicketSupplierGatewayProxy}) — local dev and CI stay unchanged.
 *
 * <p>When the flag is on, this config produces:
 * <ul>
 *   <li>A {@link WsepClient} with configured timeouts.</li>
 *   <li>A {@code @Primary} {@link IPaymentGateway} backed by
 *       {@link WsepPaymentGateway} — overrides the stub.</li>
 *   <li>A {@code @Primary} {@link ITicketSupplierGateway} backed by
 *       {@link WsepTicketSupplierGateway} — overrides the stub.</li>
 * </ul>
 *
 * <p>Switching environments is therefore a one-line change in
 * {@code application.properties} ({@code wsep.enabled=true}) — no code edit
 * required, matching the V3 spec's "external endpoints are configurable, not
 * hard-coded" requirement.
 */
@Configuration
@EnableConfigurationProperties(WsepProperties.class)
@ConditionalOnProperty(prefix = "wsep", name = "enabled", havingValue = "true")
public class WsepConfig {

    @Bean
    public WsepClient wsepClient(WsepProperties props, RestTemplateBuilder builder) {
        return new WsepClient(props, builder);
    }

    @Bean
    @Primary
    public IPaymentGateway wsepPaymentGateway(WsepClient client) {
        return new WsepPaymentGateway(client);
    }

    @Bean
    @Primary
    public ITicketSupplierGateway wsepTicketSupplierGateway(WsepClient client) {
        return new WsepTicketSupplierGateway(client);
    }
}
