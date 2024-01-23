package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.ContentGridAuditEventConfiguration;
import com.contentgrid.spring.audit.handler.AuditEventHandler;
import com.contentgrid.spring.audit.handler.LoggingAuditHandler;
import com.contentgrid.spring.boot.autoconfigure.data.web.ContentGridSpringDataRestAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.NoneNestedConditions;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

@AutoConfiguration(
        after = {
                ContentGridSpringDataRestAutoConfiguration.class,
                RepositoryRestMvcAutoConfiguration.class
        }
)
@ConditionalOnClass({
        ContentGridAuditEventConfiguration.class,
        RepositoryRestMvcConfiguration.class
})
@ConditionalOnBean(RepositoryRestMvcConfiguration.class)
@Import(ContentGridAuditEventConfiguration.class)
public class ContentGridAuditLoggingAutoConfiguration {

    @Conditional(LoggingAuditHandlerCondition.class)
    @Bean
    LoggingAuditHandler loggingAuditHandler() {
        return new LoggingAuditHandler();
    }

    // Conditions when LoggingAuditHandler is enabled:
    // - contentgrid.audit.log.enabled = true
    // - No other LoggingAuditHandler and contentgrid.audit.log.enabled != false

    private static class LoggingNotExplicitlyDisabledCondition extends NoneNestedConditions {

        public LoggingNotExplicitlyDisabledCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "contentgrid.audit.log.enabled", havingValue = "false")
        static class LoggingPropertyDisabled {

        }

    }

    private static class LoggingAuditHandlerCondition extends AnyNestedCondition {

        public LoggingAuditHandlerCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "contentgrid.audit.log.enabled", havingValue = "true")
        static class LoggingPropertyEnabled {

        }


        @ConditionalOnMissingBean(AuditEventHandler.class)
        @Conditional(LoggingNotExplicitlyDisabledCondition.class)
        static class NoOtherAuditHandlerPresent {

        }

    }

}
