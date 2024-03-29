package com.contentgrid.spring.boot.autoconfigure.data.audit;

import com.contentgrid.spring.boot.autoconfigure.data.audit.JpaAuditingAutoConfiguration.AuditorAwareConfiguration;
import com.contentgrid.spring.data.support.auditing.v1.DefaultAuditorAware;
import com.contentgrid.spring.data.support.auditing.v1.JwtAuditorAware;
import com.contentgrid.spring.data.support.auditing.v1.UserMetadata;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

@AutoConfiguration(after = {HibernateJpaAutoConfiguration.class})
@ConditionalOnBean(EntityManagerFactory.class)
@EnableJpaAuditing
@Import(AuditorAwareConfiguration.class)
public class JpaAuditingAutoConfiguration {

    @Configuration
    @ConditionalOnClass({UserMetadata.class, SecurityContextHolder.class})
    static class AuditorAwareConfiguration {

        @Bean
        @ConditionalOnClass(JwtClaimAccessor.class)
        AuditorAware<UserMetadata> jwtAuditorAware() {
            return new JwtAuditorAware();
        }

        @Bean
        @ConditionalOnMissingClass("org.springframework.security.oauth2.jwt.JwtClaimAccessor")
        AuditorAware<UserMetadata> defaultAuditorAware() {
            return new DefaultAuditorAware();
        }
    }
}
