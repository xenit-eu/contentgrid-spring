package com.contentgrid.spring.data.support.auditing.v1;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {

    @Bean
    AuditorAware<UserMetadata> jwtAuditorAware() {
        return new JwtAuditorAware();
    }
}
