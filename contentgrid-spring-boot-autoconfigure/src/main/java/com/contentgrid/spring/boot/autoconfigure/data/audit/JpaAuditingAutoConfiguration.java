package com.contentgrid.spring.boot.autoconfigure.data.audit;

import com.contentgrid.spring.data.support.auditing.v1.JpaAuditingConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtClaimAccessor;

@AutoConfiguration(after = {HibernateJpaAutoConfiguration.class})
@ConditionalOnBean(EntityManagerFactory.class)
@ConditionalOnClass({JpaAuditingConfiguration.class, JwtClaimAccessor.class})
@Import(JpaAuditingConfiguration.class)
public class JpaAuditingAutoConfiguration {

}
