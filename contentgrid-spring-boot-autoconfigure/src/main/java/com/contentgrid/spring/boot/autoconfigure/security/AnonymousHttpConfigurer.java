package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

public class AnonymousHttpConfigurer extends AbstractHttpConfigurer<AnonymousHttpConfigurer, HttpSecurity> {

    @Override
    public void init(HttpSecurity http) throws Exception {
        ApplicationContext context = http.getSharedObject(ApplicationContext.class);

        var disableCsrf = context.getEnvironment().getProperty("contentgrid.security.csrf.disabled", Boolean.class);
        if (Boolean.TRUE.equals(disableCsrf)) {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        var allowUnauthenticated = context.getEnvironment().getProperty("contentgrid.security.unauthenticated.allow", Boolean.class);
        if (Boolean.TRUE.equals(allowUnauthenticated)) {
            http.anonymous(
                    anonymous -> anonymous.authenticationFilter(new AnonymousUsernamePasswordAuthenticationFilter()));
        }
    }

    static class AnonymousUsernamePasswordAuthenticationFilter extends AnonymousAuthenticationFilter {

        public AnonymousUsernamePasswordAuthenticationFilter() {
            super(getKey(), "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        }

        @Override
        protected Authentication createAuthentication(HttpServletRequest request) {
            return new UsernamePasswordAuthenticationToken(getPrincipal(), null, getAuthorities());
        }

        private static String getKey() {
            return UUID.randomUUID().toString();
        }
    }
}