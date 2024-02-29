package com.contentgrid.spring.test.fixture.invoicing;

import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import com.contentgrid.spring.test.fixture.invoicing.model.support.UserMetadata;
import com.contentgrid.spring.test.fixture.invoicing.repository.PromotionCampaignRepository;
import com.contentgrid.spring.test.fixture.invoicing.security.AuditorAwareImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
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
    public AuditorAware<UserMetadata> auditorProvider() {
        return new AuditorAwareImpl();
    }
}
