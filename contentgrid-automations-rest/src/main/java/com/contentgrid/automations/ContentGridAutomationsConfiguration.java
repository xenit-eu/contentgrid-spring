package com.contentgrid.automations;

import com.contentgrid.automations.rest.AutomationAnnotationRepresentationModelAssembler;
import com.contentgrid.automations.rest.AutomationRepresentationModelAssembler;
import com.contentgrid.automations.rest.AutomationsRestController;
import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import com.contentgrid.thunx.spring.data.context.AbacContextSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;

@Import({
        AutomationRepresentationModelAssembler.class,
        AutomationAnnotationRepresentationModelAssembler.class
})
@Configuration(proxyBeanMethods = false)
public class ContentGridAutomationsConfiguration {

    private static final String AUTOMATIONS_RESOURCE = "classpath:automation/automations.json";

    @Bean
    AutomationsRestController automationsRestController(
            ResourceLoader resourceLoader,
            AutomationRepresentationModelAssembler assembler,
            AbacContextSupplier abacContextSupplier
    ) {
        return new AutomationsRestController(
                resourceLoader.getResource(AUTOMATIONS_RESOURCE),
                assembler,
                abacContextSupplier
        );
    }

    @Bean
    CurieProviderCustomizer automationCurieProvider() {
        return CurieProviderCustomizer.register(AutomationLinkRelations.CURIE, AutomationLinkRelations.TEMPLATE);
    }

}
