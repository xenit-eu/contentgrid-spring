package com.contentgrid.spring.boot.autoconfigure.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.integration.events.EntityChangeHibernateEventListener;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class EventsAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EventsAutoConfiguration.class, RepositoryRestMvcAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Configuration
    @AutoConfigurationPackage
    @DataJpaTest
    public static class TestConfig {


    }

    @Test
    void checkContentGridPublisher_beanExists() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasSingleBean(EntityChangeHibernateEventListener.class);
                });
    }

    @Test
    void withoutContentGridPublisherEventListener_onClasspath() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(EntityChangeHibernateEventListener.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(EntityChangeHibernateEventListener.class);
                });
    }
}