package com.contentgrid.spring.boot.autoconfigure.security;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

@Slf4j
@RequiredArgsConstructor
class OidcTestServer {

    final static WireMockServer WIREMOCK;

    static {
        WIREMOCK = new WireMockServer(options().dynamicPort());
        WIREMOCK.addMockServiceRequestListener(OidcTestServer::requestReceived);
    }

    private static void requestReceived(Request request, Response response) {
        log.info("HTTP {} - {} {} ", response.getStatus(), request.getMethod(), request.getUrl());
    }

    URI setupRealmIssuer(@NonNull String issuerName, RSAKey key) {
        return this.setupRealmIssuer(issuerName, new JWKSet(key));
    }

    URI setupRealmIssuer(@NonNull String issuerName, JWKSet keySet) {
        // setup OIDC metadata endpoint
        var issuerUri = URI.create("http://localhost:%s/%s".formatted(WIREMOCK.port(), issuerName));
        WIREMOCK.stubFor(
                WireMock.get("/%s/.well-known/openid-configuration" .formatted(issuerName))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(json(oidcProviderMetadata(issuerUri)))
                                .withStatus(200)));

        // setup JWKS endpoint
        WIREMOCK.stubFor(
                WireMock.get("/%s/.well-known/jwks.json".formatted(issuerName))
                        .willReturn(WireMock.aResponse()
                                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .withBody(keySet.toString(true))
                                .withStatus(200)));

        return issuerUri;
    }

    private Map<String, Object> oidcProviderMetadata(URI issuer) {
        Map<String, Object> response = new HashMap<>();
        response.put("issuer", issuer);
        response.put("jwks_uri", issuer + "/.well-known/jwks.json");

        response.put("authorization_endpoint", "https://example.com/o/oauth2/v2/auth");
        response.put("claims_supported", List.of());
        response.put("code_challenge_methods_supported", List.of());
        response.put("id_token_signing_alg_values_supported", List.of());
        response.put("response_types_supported", List.of());
        response.put("revocation_endpoint", "https://example.com/o/oauth2/revoke");
        response.put("scopes_supported", List.of("openid"));
        response.put("subject_types_supported", List.of("public"));
        response.put("grant_types_supported", List.of("authorization_code"));
        response.put("token_endpoint", "https://example.com/oauth2/v4/token");
        response.put("token_endpoint_auth_methods_supported", List.of("client_secret_basic"));
        response.put("userinfo_endpoint", "https://example.com/oauth2/v3/userinfo");
        return response;
    }


    @SneakyThrows
    private String json(Object object) {
        return new ObjectMapper().writeValueAsString(object);
    }

    void reset() {
        WIREMOCK.resetAll();
    }

    void start() {
        this.WIREMOCK.start();
    }


    void stop() {
        this.WIREMOCK.stop();
    }
}
