package com.contentgrid.spring.data.support.auditing.v1;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

public class JwtAuditorAware implements AuditorAware<UserMetadata> {

    @Override
    public Optional<UserMetadata> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(user -> user instanceof JwtClaimAccessor)
                .map(JwtClaimAccessor.class::cast)
                .map(jwt -> new UserMetadata(jwt.getSubject(), jwt.getIssuer().toString(), jwt.getClaimAsString("name")));
    }
}
