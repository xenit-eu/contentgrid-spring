package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.ConditionalOnDefaultWebSecurity;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@AutoConfiguration(before = { OAuth2ResourceServerAutoConfiguration.class, SecurityAutoConfiguration.class })
@ConditionalOnDefaultWebSecurity
@ConditionalOnProperty("contentgrid.spring.security.allow-anonymous")
@EnableWebSecurity
public class AnonymousTestAutoConfiguration {

    private static final String ANONYMOUS_NAME = "anonymous";
    private static final String ANONYMOUS_AUTHORITY = "ANONYMOUS";
    private static final String ANONYMOUS_ISSUER = "http://localhost/realms/0";
    private static final String ANONYMOUS_PASSWORD = "password";

    @Bean
    @ConditionalOnClass(BearerTokenAuthenticationToken.class)
    public SecurityFilterChain anonymousJwtFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(anonymous -> anonymous.authenticationFilter(new AnonymousJwtAuthenticationFilter()))
                .build();
    }

    @Bean
    @ConditionalOnMissingClass("org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken")
    public SecurityFilterChain anonymousUsernamePasswordFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .anonymous(anonymous -> anonymous.authenticationFilter(
                        new AnonymousUsernamePasswordAuthenticationFilter()))
                .build();
    }

    private static String getKey() {
        return UUID.randomUUID().toString();
    }

    static class AnonymousJwtAuthenticationFilter extends AnonymousAuthenticationFilter {

        public AnonymousJwtAuthenticationFilter() {
            super(getKey(), ANONYMOUS_NAME, AuthorityUtils.createAuthorityList("SCOPE_" + ANONYMOUS_AUTHORITY));
        }

        @Override
        protected Authentication createAuthentication(HttpServletRequest request) {
            var username = getPrincipal().toString();
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(username)
                    .issuer(ANONYMOUS_ISSUER)
                    .claim("scope", ANONYMOUS_AUTHORITY.toLowerCase(Locale.ROOT))
                    .claim("name", username)
                    .build();

            return new JwtAuthenticationToken(jwt, getAuthorities());
        }

    }

    static class AnonymousUsernamePasswordAuthenticationFilter extends AnonymousAuthenticationFilter {

        public AnonymousUsernamePasswordAuthenticationFilter() {
            super(getKey(), ANONYMOUS_NAME, AuthorityUtils.createAuthorityList("ROLE_" + ANONYMOUS_AUTHORITY));
        }

        @Override
        protected Authentication createAuthentication(HttpServletRequest request) {
            var username = getPrincipal().toString();

            return new UsernamePasswordAuthenticationToken(username, ANONYMOUS_PASSWORD, getAuthorities());
        }
    }
}
