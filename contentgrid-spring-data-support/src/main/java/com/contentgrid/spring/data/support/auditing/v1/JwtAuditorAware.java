package com.contentgrid.spring.data.support.auditing.v1;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

public class JwtAuditorAware extends DefaultAuditorAware {

    @Override
    public Optional<UserMetadata> getCurrentAuditor() {
        var result = Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(user -> user instanceof JwtClaimAccessor)
                .map(JwtClaimAccessor.class::cast)
                .filter(jwt -> jwt.getSubject() != null)
                .filter(jwt -> jwt.getIssuer() != null)
                .map(jwt -> new UserMetadata(jwt.getSubject(), jwt.getIssuer().toString(),
                        jwt.hasClaim("name") ? jwt.getClaimAsString("name") : jwt.getSubject()));
        return result.isEmpty() ? super.getCurrentAuditor() : result;
    }
}
