package com.sdnah.Ticket_Management_System_.wsep;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.IPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.Order.ITicketSupplierGateway;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.PaymentGatewayProxy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.TicketSupplierGatewayProxy;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepClient;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepConfig;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepPaymentGateway;
import com.sdnah.Ticket_Management_System_.Backend.Infastructure_Layer.wsep.WsepTicketSupplierGateway;

/**
 * Verifies the {@code wsep.enabled} flag actually swaps the gateway
 * implementations at the Spring container level — without booting the full
 * application or making any real HTTP call.
 */
@DisplayName("WsepConfig — Spring wiring switch")
class WsepConfigWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
            .withUserConfiguration(WsepConfig.class,
                    PaymentGatewayProxy.class,
                    TicketSupplierGatewayProxy.class);

    @Test
    @DisplayName("With wsep.enabled unset, no WSEP beans exist and the in-memory stubs win")
    void disabledByDefault_stubsWin() {
        contextRunner
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(WsepClient.class);
                    assertThat(ctx).doesNotHaveBean(WsepPaymentGateway.class);
                    assertThat(ctx).doesNotHaveBean(WsepTicketSupplierGateway.class);

                    // The autowired IPaymentGateway is the stub
                    assertThat(ctx.getBean(IPaymentGateway.class))
                            .isInstanceOf(PaymentGatewayProxy.class);
                    assertThat(ctx.getBean(ITicketSupplierGateway.class))
                            .isInstanceOf(TicketSupplierGatewayProxy.class);
                });
    }

    @Test
    @DisplayName("With wsep.enabled=false explicitly, stubs still win")
    void explicitlyDisabled_stubsWin() {
        contextRunner
                .withPropertyValues("wsep.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(WsepClient.class);
                    assertThat(ctx.getBean(IPaymentGateway.class))
                            .isInstanceOf(PaymentGatewayProxy.class);
                });
    }

    @Test
    @DisplayName("With wsep.enabled=true, the @Primary WSEP gateways take over")
    void enabled_wsepWins() {
        contextRunner
                .withPropertyValues(
                        "wsep.enabled=true",
                        "wsep.base-url=http://example.test/")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(WsepClient.class);
                    assertThat(ctx).hasSingleBean(WsepPaymentGateway.class);
                    assertThat(ctx).hasSingleBean(WsepTicketSupplierGateway.class);

                    // @Primary wins over the stub
                    assertThat(ctx.getBean(IPaymentGateway.class))
                            .isInstanceOf(WsepPaymentGateway.class);
                    assertThat(ctx.getBean(ITicketSupplierGateway.class))
                            .isInstanceOf(WsepTicketSupplierGateway.class);
                });
    }
}
