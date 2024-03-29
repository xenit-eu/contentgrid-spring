package com.contentgrid.spring.data.support.auditing.v1;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

public class DefaultAuditorAware implements AuditorAware<UserMetadata> {

    @Override
    public Optional<UserMetadata> getCurrentAuditor() {
        return Optional.ofNullable(SecurityContextHolder.getContext())
                .map(SecurityContext::getAuthentication)
                .filter(Authentication::isAuthenticated)
                .map(authentication ->
                        new UserMetadata(authentication.getName(), null, authentication.getName()));
    }
}
