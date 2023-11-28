package com.contentgrid.spring.boot.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.contentgrid.spring.boot.autoconfigure.security.ManagementContextSupplierConfiguration.ManagementContextSupplier;
import java.net.InetAddress;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcConfigurer;
import org.springframework.test.web.servlet.setup.MockMvcConfigurerAdapter;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
class ActuatorEndpointsWebSecurityAutoConfigurationTest {

    static final String ACTUATOR_ROOT = "/actuator";
    static final String ACTUATOR_HEALTH = "/actuator/health";
    static final String ACTUATOR_INFO = "/actuator/info";
    static final String ACTUATOR_METRICS = "/actuator/metrics";
    static final String ACTUATOR_ENV = "/actuator/env";

    static class AutoConfigs {

        static final AutoConfigurations ACTUATORS = AutoConfigurations.of(
                HealthContributorAutoConfiguration.class,
                HealthEndpointAutoConfiguration.class,

                InfoEndpointAutoConfiguration.class,

                MetricsAutoConfiguration.class,
                MetricsEndpointAutoConfiguration.class,
                CompositeMeterRegistryAutoConfiguration.class,

                EnvironmentEndpointAutoConfiguration.class,

                EndpointAutoConfiguration.class,
                WebMvcAutoConfiguration.class,
                WebEndpointAutoConfiguration.class
        );

        static final AutoConfigurations MANAGEMENT = AutoConfigurations.of(
                ManagementContextAutoConfiguration.class,
                ServletManagementContextAutoConfiguration.class,
                ServletWebServerFactoryAutoConfiguration.class,
                DispatcherServletAutoConfiguration.class
        );
    }

    WebApplicationContextRunner runner = new WebApplicationContextRunner(
            AnnotationConfigServletWebServerApplicationContext::new)
            .withPropertyValues(
                    "management.endpoints.web.exposure.include=*"
            )
            .withInitializer(new ServerPortInfoApplicationContextInitializer())
            .withConfiguration(AutoConfigs.ACTUATORS)
            .withConfiguration(AutoConfigs.MANAGEMENT)
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    ManagementWebSecurityAutoConfiguration.class,
                    ActuatorEndpointsWebSecurityAutoConfiguration.class)
            );

    @Test
    void whenAccessFromRemoteAddress() {

        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("actuatorEndpointsSecurityFilterChain");

            withWebTestClient(context, remoteAddress("192.0.2.1"), assertHttp -> {
                // public endpoints
                assertHttp.get(ACTUATOR_HEALTH).isOk();
                assertHttp.get(ACTUATOR_INFO).isOk();

                // management on primary server port
                assertHttp.get(ACTUATOR_METRICS).isForbidden();

                // other endpoints fall through if not from loopback-address
                assertHttp.get(ACTUATOR_ENV).isForbidden();

                // root forbidden
                assertHttp.get(ACTUATOR_ROOT).isForbidden();
            });
        });
    }

    @Test
    void whenManagementOnDifferentPort() {

        runner.withPropertyValues("management.server.port=0")

                .run(context -> {

                    assertThat(context)
                            .hasNotFailed()
                            .hasBean("actuatorEndpointsSecurityFilterChain");

                    withWebTestClient(context, remoteAddress("192.0.2.1"), assertHttp -> {
                        // public endpoints
                        assertHttp.get(ACTUATOR_HEALTH).isOk();
                        assertHttp.get(ACTUATOR_INFO).isOk();

                        // management on different port
                        assertHttp.get(ACTUATOR_METRICS).isOk();

                        // other endpoints fall through if not from loopback-address
                        assertHttp.get(ACTUATOR_ENV).isForbidden();

                        // root forbidden
                        assertHttp.get(ACTUATOR_ROOT).isForbidden();
                    });
                });
    }

    @Test
    void whenAccessFromLocalAddress() {

        runner.run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("actuatorEndpointsSecurityFilterChain");

            withWebTestClient(context, remoteAddress("localhost"), assertHttp -> {
                // all /actuator endpoitns allowed when from a loopback address
                assertHttp.get(ACTUATOR_HEALTH).isOk();
                assertHttp.get(ACTUATOR_INFO).isOk();
                assertHttp.get(ACTUATOR_METRICS).isOk();
                assertHttp.get(ACTUATOR_ENV).isOk();

                assertHttp.get(ACTUATOR_ROOT).isOk();
            });
        });
    }

    private void withWebTestClient(ApplicationContext context, MockMvcConfigurer remoteAddress,
            Consumer<HttpAssertClient> callback) {
        WebTestClient client;

        var managementContext = context.getBean(ManagementContextSupplier.class).get();
        client = MockMvcWebTestClient.bindToApplicationContext(Objects.requireNonNull(managementContext))
                .apply(remoteAddress)
                .apply(springSecurity())
                .build();

        callback.accept((@NonNull String endpoint) ->
                client.get().uri(endpoint).accept(MediaType.APPLICATION_JSON).exchange().expectStatus());
    }

    @FunctionalInterface
    interface HttpAssertClient {

        StatusAssertions get(String endpoint);
    }

    @SneakyThrows
    private MockMvcConfigurer remoteAddress(String remoteAddress) {
        var address = InetAddress.getByName(remoteAddress);
        return new RemoteAddressMockMvcConfigurer(address);
    }

    @RequiredArgsConstructor
    static class RemoteAddressMockMvcConfigurer extends MockMvcConfigurerAdapter {

        @NonNull
        final InetAddress remoteAddress;

        @Override
        @Nullable
        public RequestPostProcessor beforeMockMvcCreated(ConfigurableMockMvcBuilder<?> builder,
                WebApplicationContext cxt) {
            return request -> {
                request.setRemoteAddr(remoteAddress.getHostAddress());
                request.setRemoteHost(remoteAddress.getHostName());
                return request;
            };
        }
    }
}