package com.contentgrid.spring.boot.autoconfigure.data.audit;

import com.contentgrid.spring.data.support.auditing.v1.JwtAuditorAware;
import com.contentgrid.spring.data.support.auditing.v1.UserMetadata;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

@AutoConfiguration(before = JpaAuditingAutoConfiguration.class)
@ConditionalOnMissingBean(AuditorAware.class)
@ConditionalOnClass({UserMetadata.class, SecurityContextHolder.class, JwtClaimAccessor.class})
public class JwtAuditorAwareAutoConfiguration {

    @Bean
    AuditorAware<UserMetadata> jwtAuditorAware() {
        return new JwtAuditorAware();
    }
}
