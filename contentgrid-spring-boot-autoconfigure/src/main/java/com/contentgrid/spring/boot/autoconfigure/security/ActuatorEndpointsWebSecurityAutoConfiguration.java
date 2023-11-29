package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.EndpointRequestMatcher;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.boot.actuate.metrics.export.prometheus.PrometheusScrapeEndpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


@AutoConfiguration(
        after = {
                HealthEndpointAutoConfiguration.class,
                InfoEndpointAutoConfiguration.class,
                WebEndpointAutoConfiguration.class,
                SecurityAutoConfiguration.class
        })
@ConditionalOnClass(value = {HealthEndpoint.class, SecurityFilterChain.class, HttpSecurity.class})
@ConditionalOnWebApplication(type = Type.SERVLET)
public class ActuatorEndpointsWebSecurityAutoConfiguration {

    /**
     * List of publicly accessible management endpoints
     */
    private static final EndpointRequestMatcher PUBLIC_ENDPOINTS = EndpointRequest.to(
            InfoEndpoint.class,
            HealthEndpoint.class
    );

    /**
     * List of management metrics endpoints, allowed when the management port and server port are different.
     */
    private static final EndpointRequestMatcher METRICS_ENDPOINTS = EndpointRequest.to(
            MetricsEndpoint.class,
            PrometheusScrapeEndpoint.class
    );

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain actuatorEndpointsSecurityFilterChain(HttpSecurity http, Environment environment)
            throws Exception {

        http.authorizeHttpRequests((requests) -> requests.requestMatchers(
                        PUBLIC_ENDPOINTS,
                        new AndRequestMatcher(
                                METRICS_ENDPOINTS,
                                request -> ManagementPortType.get(environment) == ManagementPortType.DIFFERENT
                        ),
                        new AndRequestMatcher(
                                EndpointRequest.toAnyEndpoint(),
                                new LoopbackInetAddressMatcher()
                        )
                )
                .permitAll());

        // all the other /actuator endpoints fall through
        return http.build();
    }

    private static class LoopbackInetAddressMatcher implements RequestMatcher {

        @Override
        public boolean matches(HttpServletRequest request) {
            return isLoopbackAddress(request.getRemoteAddr());
        }

        boolean isLoopbackAddress(String address) {
            try {
                var remoteAddress = InetAddress.getByName(address);
                return remoteAddress.isLoopbackAddress();
            } catch (UnknownHostException ex) {
                return false;
            }
        }
    }
}
