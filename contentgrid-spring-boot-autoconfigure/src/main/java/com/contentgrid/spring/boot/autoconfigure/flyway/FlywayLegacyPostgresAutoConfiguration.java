package com.contentgrid.spring.boot.autoconfigure.flyway;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigureBefore(FlywayAutoConfiguration.class)
@ConditionalOnClass({Flyway.class, PostgreSQLConfigurationExtension.class})
public class FlywayLegacyPostgresAutoConfiguration {

    /**
     * The Flyway default of transactional lock enabled conflicts with 'CREATE INDEX CONCURRENTLY' migrations, so it should be disabled by default.
     *
     * @see <a href="https://documentation.red-gate.com/fd/postgresql-transactional-lock-184127530.html">Flyway documentation<a>
     */
    @Bean
    FlywayConfigurationCustomizer postgresqlLegacyFlywayConfigurationCustomizerDisableTransactionalLock() {
        return configuration -> {
            configuration.getPluginRegister().getPlugin(PostgreSQLConfigurationExtension.class).setTransactionalLock(false);
        };
    }
}
