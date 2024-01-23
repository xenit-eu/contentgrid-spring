package com.contentgrid.spring.test.fixture.invoicing;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

@SpringBootApplication
public class InvoicingApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicingApplication.class, args);
    }

    @Bean
    CurieProviderCustomizer datamodelCurieProviderCustomizer() {
        return CurieProviderCustomizer.register("d", "https://contentgrid.cloud/rels/datamodel/{rel}");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDatabaseConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgreSQLContainer() {
            return new PostgreSQLContainer();
        }

        @Bean
        @ServiceConnection
        RabbitMQContainer rabbitMQContainer() {
            return new RabbitMQContainer();
        }
    }

}
