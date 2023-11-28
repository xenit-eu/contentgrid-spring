package com.contentgrid.spring.boot.autoconfigure.security;

import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

@RequiredArgsConstructor
@ManagementContextConfiguration(proxyBeanMethods = false)
class ManagementContextSupplierConfiguration {

    private final ConfigurableWebApplicationContext managementContext;

    @PostConstruct
    void registerManagementContextSupplierInParentContext() {
        ConfigurableApplicationContext rootContext = managementContext;
        if (managementContext.getParent() != null) {
            rootContext = (ConfigurableApplicationContext) managementContext.getParent();
        }
        Objects.requireNonNull(rootContext).getBeanFactory()
                .registerSingleton("management-context-supplier",
                        (ManagementContextSupplier) () -> (WebApplicationContext) managementContext);
    }

    @FunctionalInterface
    interface ManagementContextSupplier extends Supplier<WebApplicationContext> {

    }

}
