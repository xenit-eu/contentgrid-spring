package com.contentgrid.spring.boot.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.contentgrid.spring.boot.actuator.policy.PolicyActuator;
import com.contentgrid.spring.boot.actuator.webhooks.WebHooksConfigActuator;
import com.contentgrid.spring.boot.autoconfigure.actuator.ActuatorAutoConfiguration;
import com.contentgrid.spring.boot.autoconfigure.security.ManagementContextSupplierConfiguration.ManagementContextSupplier;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.reactive.server.StatusAssertions;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
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

    static final String ACTUATOR_POLICY = "/actuator/policy";
    static final String ACTUATOR_WEBHOOK = "/actuator/webhooks";

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

        static final AutoConfigurations CONTENTGRID_ACTUATORS = AutoConfigurations.of(
                ActuatorAutoConfiguration.class
        );
    }

    WebApplicationContextRunner runner = new WebApplicationContextRunner(
            AnnotationConfigServletWebServerApplicationContext::new)
            .withPropertyValues(
                    "server.port=0",
                    "management.endpoints.web.exposure.include=*",
                    "contentgrid.system.policyPackage=foobar"
            )
            .withInitializer(new ServerPortInfoApplicationContextInitializer())
            .withConfiguration(AutoConfigs.ACTUATORS)
            .withConfiguration(AutoConfigs.CONTENTGRID_ACTUATORS)
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
                    .hasBean("actuatorEndpointsSecurityFilterChain")
                    .hasSingleBean(PolicyActuator.class)
                    .hasSingleBean(WebHooksConfigActuator.class);

            withWebTestClient(context, remoteAddress("192.0.2.1"), assertHttp -> {
                // public endpoints
                assertHttp.get(ACTUATOR_HEALTH).isOk();
                assertHttp.get(ACTUATOR_INFO).isOk();

                // management on primary server port
                assertHttp.get(ACTUATOR_METRICS).isForbidden();
                assertHttp.get(ACTUATOR_POLICY).isForbidden();
                assertHttp.get(ACTUATOR_WEBHOOK).isForbidden();

                // other endpoints fall through if not from loopback-address
                assertHttp.get(ACTUATOR_ENV).isForbidden();

                // root forbidden
                assertHttp.get(ACTUATOR_ROOT).isForbidden();
            });

            // security matcher on /actuator/** and only /actuator/** endpoints
            assertThat(context.getBean("actuatorEndpointsSecurityFilterChain", SecurityFilterChain.class))
                    .isNotNull()
                    .satisfies(filterChain -> {
                        var servletContext = context.getServletContext();
                        assertThat(context.getServletContext()).isNotNull();

                        assertThat(filterChain.matches(get("/actuator", servletContext))).isTrue();
                        assertThat(filterChain.matches(get("/actuator/health", servletContext))).isTrue();
                        assertThat(filterChain.matches(get("/api", servletContext))).isFalse();
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
                        assertHttp.get(ACTUATOR_POLICY).isOk();
                        assertHttp.get(ACTUATOR_WEBHOOK).isOk();

                        // other endpoints fall through if not from loopback-address
                        assertHttp.get(ACTUATOR_ENV).isForbidden();

                        // root forbidden
                        assertHttp.get(ACTUATOR_ROOT).isForbidden();
                    });

                    // security matcher on /actuator/** on management port only
                    assertThat(context.getBean("actuatorEndpointsSecurityFilterChain", SecurityFilterChain.class))
                            .isNotNull()
                            .satisfies(filterChain -> {
                                var servletContext = context.getServletContext();
                                assertThat(context.getServletContext()).isNotNull();

                                assertThat(filterChain.matches(get("/actuator", servletContext))).isFalse();
                                assertThat(filterChain.matches(get("/actuator/health", servletContext))).isFalse();
                                assertThat(filterChain.matches(get("/api", servletContext))).isFalse();

                                // management context runs on a different port
                                var mgmtServletContext = context.getBean(ManagementContextSupplier.class).get()
                                        .getServletContext();
                                assertThat(filterChain.matches(get("/actuator", mgmtServletContext))).isTrue();
                                assertThat(filterChain.matches(get("/actuator/health", mgmtServletContext))).isTrue();
                                assertThat(filterChain.matches(get("/api", mgmtServletContext))).isFalse();

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
                // all /actuator endpoints allowed when from a loopback address
                assertHttp.get(ACTUATOR_HEALTH).isOk();
                assertHttp.get(ACTUATOR_INFO).isOk();
                assertHttp.get(ACTUATOR_METRICS).isOk();
                assertHttp.get(ACTUATOR_POLICY).isOk();
                assertHttp.get(ACTUATOR_WEBHOOK).isOk();
                assertHttp.get(ACTUATOR_ENV).isOk();

                assertHttp.get(ACTUATOR_ROOT).isOk();
            });

            // should match /actuator/** and only /actuator/** endpoints
            assertThat(context.getBean("actuatorEndpointsSecurityFilterChain", SecurityFilterChain.class))
                    .isNotNull()
                    .satisfies(filterChain -> {
                        var servletContext = context.getServletContext();
                        assertThat(context.getServletContext()).isNotNull();

                        assertThat(filterChain.matches(get("/actuator", servletContext))).isTrue();
                        assertThat(filterChain.matches(get("/actuator/health", servletContext))).isTrue();
                        assertThat(filterChain.matches(get("/api", servletContext))).isFalse();
                    });
        });
    }

    @Test
    void withoutContentGridActuatorsOnClasspath() {

        runner
                .withPropertyValues("management.server.port=0")
                .withClassLoader(new FilteredClassLoader(PolicyActuator.class))
                .run(context -> {
            assertThat(context)
                    .hasNotFailed()
                    .hasBean("actuatorEndpointsSecurityFilterChain")
                    .doesNotHaveBean(PolicyActuator.class)
                    .doesNotHaveBean(WebHooksConfigActuator.class);

            // security matcher on /actuator/** and only /actuator/** endpoints
            // because contentgrid actuators are not loaded
            // the contentgrid-actuators are not covered by EndpointRequest.anyEndpoint()
            assertThat(context.getBean("actuatorEndpointsSecurityFilterChain", SecurityFilterChain.class))
                    .isNotNull()
                    .satisfies(filterChain -> {
                        var servletContext = context.getServletContext();
                        assertThat(context.getServletContext()).isNotNull();

                        assertThat(filterChain.matches(get(ACTUATOR_ROOT, servletContext))).isFalse();
                        assertThat(filterChain.matches(get(ACTUATOR_HEALTH, servletContext))).isFalse();
                        assertThat(filterChain.matches(get(ACTUATOR_POLICY, servletContext))).isFalse();
                        assertThat(filterChain.matches(get(ACTUATOR_WEBHOOK, servletContext))).isFalse();

                        // management context runs on a different port
                        var mgmtServletContext = context.getBean(ManagementContextSupplier.class).get()
                                .getServletContext();
                        assertThat(filterChain.matches(get(ACTUATOR_ROOT, mgmtServletContext))).isTrue();
                        assertThat(filterChain.matches(get(ACTUATOR_HEALTH, mgmtServletContext))).isTrue();
                        assertThat(filterChain.matches(get(ACTUATOR_POLICY, mgmtServletContext))).isFalse();
                        assertThat(filterChain.matches(get(ACTUATOR_WEBHOOK, mgmtServletContext))).isFalse();
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
                client.get().uri(endpoint).accept(MediaType.APPLICATION_JSON, MediaType.ALL).exchange().expectStatus());
    }

    private static HttpServletRequest get(String endpoint, ServletContext servletContext) {
        return MockMvcRequestBuilders.get(endpoint).buildRequest(servletContext);
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
        public RequestPostProcessor beforeMockMvcCreated(@NonNull ConfigurableMockMvcBuilder<?> builder,
                @NonNull WebApplicationContext cxt) {
            return request -> {
                request.setRemoteAddr(remoteAddress.getHostAddress());
                request.setRemoteHost(remoteAddress.getHostName());
                return request;
            };
        }
    }
}