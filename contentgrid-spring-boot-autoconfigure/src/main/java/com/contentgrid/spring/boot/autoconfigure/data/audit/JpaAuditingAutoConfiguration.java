package com.contentgrid.spring.boot.autoconfigure.data.audit;

import com.contentgrid.spring.data.support.auditing.v1.DefaultAuditorAware;
import com.contentgrid.spring.data.support.auditing.v1.JwtAuditorAware;
import com.contentgrid.spring.data.support.auditing.v1.UserMetadata;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.AuditingHandler;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

@AutoConfiguration(after = {HibernateJpaAutoConfiguration.class})
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnMissingBean(AuditingHandler.class)
@EnableJpaAuditing
class JpaAuditingAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    @ConditionalOnClass({UserMetadata.class, SecurityContextHolder.class, JwtClaimAccessor.class})
    AuditorAware<UserMetadata> jwtAuditorAware() {
        return new JwtAuditorAware();
    }

    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    @ConditionalOnClass({UserMetadata.class, SecurityContextHolder.class})
    AuditorAware<UserMetadata> defaultAuditorAware() {
        return new DefaultAuditorAware();
    }
}
