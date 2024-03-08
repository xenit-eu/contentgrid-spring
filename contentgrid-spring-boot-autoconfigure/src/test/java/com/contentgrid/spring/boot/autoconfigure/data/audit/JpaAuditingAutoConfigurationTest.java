package com.contentgrid.spring.boot.autoconfigure.data.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.data.support.auditing.v1.JpaAuditingConfiguration;
import com.contentgrid.spring.data.support.auditing.v1.JwtAuditorAware;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

class JpaAuditingAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                    TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
                    JpaAuditingAutoConfiguration.class
            ));

    @Test
    void checkDefaults() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JpaAuditingConfiguration.class);
                    assertThat(context).hasSingleBean(JwtAuditorAware.class);
                    assertThat(context).hasSingleBean(AuditingEntityListener.class);
                });
    }

    @Test
    void checkWithoutDependency() {
        contextRunner.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
                .withClassLoader(new FilteredClassLoader(JpaAuditingConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(JpaAuditingConfiguration.class);
                    assertThat(context).doesNotHaveBean(JwtAuditorAware.class);
                    assertThat(context).doesNotHaveBean(AuditingEntityListener.class);
                });
    }
}