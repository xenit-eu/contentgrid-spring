package com.contentgrid.spring.test.fixture.invoicing.security;

import com.contentgrid.spring.test.fixture.invoicing.model.support.UserMetadata;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

@NoArgsConstructor
public class AuditorAwareImpl implements AuditorAware<UserMetadata> {

    @Override
    @NotNull
    public Optional<UserMetadata> getCurrentAuditor() {
        try {
            return Optional.ofNullable(SecurityContextHolder.getContext())
                    .map(SecurityContext::getAuthentication)
                    .filter(Authentication::isAuthenticated)
                    .map(Authentication::getPrincipal)
                    .map(User.class::cast)
                    .map(user -> new UserMetadata("12345", user.getUsername()));
        } catch (ClassCastException exception) {
            // User.class::cast fails with @WithAnonymousUser
            // because the principal is then the String "anonymous".
            // Just return empty optional when the user is anonymous.
            return Optional.empty();
        }
    }
}