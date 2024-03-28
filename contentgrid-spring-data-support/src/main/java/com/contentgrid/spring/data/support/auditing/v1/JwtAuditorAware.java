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
                .map(authentication -> {
                    if (authentication.getPrincipal() instanceof JwtClaimAccessor jwt) {
                        if (jwt.getSubject() != null && jwt.getIssuer() != null) {
                            return new UserMetadata(jwt.getSubject(), jwt.getIssuer().toString(),
                                    jwt.hasClaim("name") ? jwt.getClaimAsString("name") : jwt.getSubject());
                        }
                        return null;
                    }
                    return new UserMetadata(authentication.getName(), null, authentication.getName());
                });
    }
}
