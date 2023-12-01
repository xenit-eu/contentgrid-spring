package com.contentgrid.spring.boot.autoconfigure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.Filter;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.BeanIds;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

@WireMockTest
class MultiTenantOAuth2ResourceServerAutoConfigurationTest {

    AutoConfigurations WEB = AutoConfigurations.of(
            WebMvcAutoConfiguration.class,
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class

    );

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(WEB)
            .withConfiguration(AutoConfigurations.of(
                    SecurityAutoConfiguration.class,
                    OAuth2ResourceServerAutoConfiguration.class,
                    MultiTenantOAuth2ResourceServerAutoConfiguration.class
            ));
    final static OidcTestServer OIDC_SERVER = new OidcTestServer();

    @BeforeAll
    static void setup() {
        OIDC_SERVER.start();
    }

    @AfterAll
    static void close() {
        OIDC_SERVER.stop();
    }

    @BeforeEach
    void reset() {
        OIDC_SERVER.reset();
    }


    @Test
    void singleTrustedJwtIssuer() {

        var rsaKey = genRsaKey();
        var issuer = OIDC_SERVER.setupRealmIssuer("my-issuer", rsaKey);

        this.contextRunner
                .withPropertyValues(
                        "contentgrid.security.oauth2.trusted-jwt-issuers[0]=%s" .formatted(issuer))
                .withUserConfiguration(TestController.class)
                .run((context) -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(JwtIssuerAuthenticationManagerResolver.class);

                    assertThat(getBearerTokenFilter(context)).isNotNull();

                    var client = createWebTestClient(context);

                    // happy path, valid JWT
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(rsaKey, issuer)))
                            .exchange()
                            .expectStatus().isOk();

                    // missing authorization header
                    client.get().uri("/")
                            .exchange()
                            .expectStatus().isUnauthorized();

                    // JWT signed by unknown key
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(genRsaKey(), issuer)))
                            .exchange()
                            .expectStatus().isUnauthorized();

                });
    }

    @Test
    void multipleTrustedJwtIssuers() {

        var rsaKey1 = genRsaKey();
        var issuer1 = OIDC_SERVER.setupRealmIssuer("issuer-1", rsaKey1);

        var rsaKey2 = genRsaKey();
        var issuer2 = OIDC_SERVER.setupRealmIssuer("issuer-2", rsaKey2);

        this.contextRunner
                .withPropertyValues(
                        "contentgrid.security.oauth2.trusted-jwt-issuers[0]=%s" .formatted(issuer1),
                        "contentgrid.security.oauth2.trusted-jwt-issuers[1]=%s" .formatted(issuer2)
                )
                .withUserConfiguration(TestController.class)
                .run((context) -> {
                    assertThat(context)
                            .hasNotFailed()
                            .hasSingleBean(JwtIssuerAuthenticationManagerResolver.class);

                    assertThat(getBearerTokenFilter(context)).isNotNull();

                    var client = createWebTestClient(context);

                    // happy path, valid JWT from issuer1
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(rsaKey1, issuer1)))
                            .exchange()
                            .expectStatus().isOk();

                    // happy path, valid JWT from issuer2
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(rsaKey2, issuer2)))
                            .exchange()
                            .expectStatus().isOk();

                    // expect HTTP 401 when mixing and matching
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(rsaKey1, issuer2)))
                            .exchange()
                            .expectStatus().isUnauthorized();
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(rsaKey2, issuer1)))
                            .exchange()
                            .expectStatus().isUnauthorized();

                    // JWT signed by unknown key
                    client.get().uri("/")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer %s" .formatted(jwt(genRsaKey(), issuer1)))
                            .exchange()
                            .expectStatus().isUnauthorized();

                });
    }


    @Test
    void conditionalOnTrustedJwtIssuersProperty() {
        this.contextRunner.run((context) -> assertThat(context)
                .hasNotFailed()
                .hasSingleBean(MultiTenantOAuth2ResourceServerAutoConfiguration.class)
                .doesNotHaveBean(JwtIssuerAuthenticationManagerResolver.class)
                .doesNotHaveBean(JwtDecoder.class)

                .hasSingleBean(SecurityFilterChain.class)
        );
    }

    @Test
    void defaultResourceServerJwtIssuerUri() {
        this.contextRunner
                .withPropertyValues(
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://jwk-oidc-issuer-location.com")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    // our own auto-config was a no-op
                    assertThat(context)
                            .hasSingleBean(MultiTenantOAuth2ResourceServerAutoConfiguration.class)
                            .doesNotHaveBean(JwtIssuerAuthenticationManagerResolver.class);

                    // the spring-issuer-uri still loads fine
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                    assertThat(getBearerTokenFilter(context)).isNotNull();
                });
    }

    private Filter getBearerTokenFilter(AssertableWebApplicationContext context) {
        FilterChainProxy filterChain = (FilterChainProxy) context.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
        List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
        List<Filter> filters = filterChains.get(0).getFilters();
        return filters.stream().filter((f) -> f instanceof BearerTokenAuthenticationFilter).findFirst().orElse(null);
    }

    private static WebTestClient createWebTestClient(WebApplicationContext context) {
        return MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .build();
    }

    @RestController
    static class TestController {

        @GetMapping("/")
        ResponseEntity<?> root() {
            return ResponseEntity.ok(Map.of("test", "ok"));
        }
    }

    static String jwt(@NonNull RSAKey rsaKey, @NonNull URI issuer) {
        return jwt(rsaKey, claims -> claims.issuer(issuer.toString()));
    }

    @SneakyThrows
    static String jwt(@NonNull RSAKey rsaKey, @NonNull Consumer<JWTClaimsSet.Builder> claimsCallback) {
        var claims = new JWTClaimsSet.Builder();
        claimsCallback.accept(claims);

        var jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID()).build(),
                claims.build()
        );
        jwt.sign(new RSASSASigner(rsaKey));
        return jwt.serialize();
    }

    @SneakyThrows
    static RSAKey genRsaKey() {
        return new RSAKeyGenerator(RSAKeyGenerator.MIN_KEY_SIZE_BITS).keyIDFromThumbprint(true).generate();
    }

}