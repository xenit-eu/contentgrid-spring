package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

public class AnonymousHttpConfigurer extends AbstractHttpConfigurer<AnonymousHttpConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity http) throws Exception {
        ApplicationContext context = http.getSharedObject(ApplicationContext.class);
        var disableCsrf = context.getEnvironment().getProperty("contentgrid.security.csrf.disabled", Boolean.class);
        var allowUnauthenticated = context.getEnvironment().getProperty("contentgrid.security.unauthenticated.allow", Boolean.class);
        if (Boolean.TRUE.equals(disableCsrf)) {
            http.csrf(AbstractHttpConfigurer::disable);
        }
        if (Boolean.TRUE.equals(allowUnauthenticated)) {
            if (classExists(context, "org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken")) {
                http.anonymous(anonymous -> anonymous.authenticationFilter(new AnonymousJwtAuthenticationFilter()));
            } else {
                http.anonymous(anonymous -> anonymous.authenticationFilter(
                        new AnonymousUsernamePasswordAuthenticationFilter()));
            }
        }
    }

    private boolean classExists(ApplicationContext context, String name) {
        try {
            var classLoader = context.getClassLoader();
            return classLoader != null && classLoader.loadClass(name) != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static final String ANONYMOUS_NAME = "anonymous";
    private static final String ANONYMOUS_AUTHORITY = "ANONYMOUS";
    private static final String ANONYMOUS_ISSUER = "http://localhost/realms/0";
    private static final String ANONYMOUS_PASSWORD = "password";

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