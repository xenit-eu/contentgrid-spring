package com.contentgrid.spring.test.fixture.invoicing;

import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import com.contentgrid.spring.test.fixture.invoicing.repository.PromotionCampaignRepository;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Component
@EnableJpaAuditing
class ControllerCustomization implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry corsRegistry) {

        config.withEntityLookup().
                forRepository(PromotionCampaignRepository.class, PromotionCampaign::getPromoCode, PromotionCampaignRepository::findByPromoCode);
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @NoArgsConstructor
    static class AuditorAwareImpl implements AuditorAware<String> {

        @Override
        @NotNull
        public Optional<String> getCurrentAuditor() {
            return Optional.ofNullable(SecurityContextHolder.getContext())
                    .map(SecurityContext::getAuthentication)
                    .filter(Authentication::isAuthenticated)
                    .map(Authentication::getPrincipal)
                    .map(User.class::cast)
                    .map(User::getUsername);
        }
    }
}
