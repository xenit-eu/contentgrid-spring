package com.contentgrid.spring.automations;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import com.contentgrid.spring.automations.rest.AutomationAnnotationRepresentationModelAssembler;
import com.contentgrid.spring.automations.rest.AutomationRepresentationModelAssembler;
import com.contentgrid.spring.automations.rest.AutomationsRestController;
import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import com.contentgrid.thunx.spring.data.context.AbacContextSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;

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

    @Bean
    RepresentationModelProcessor<RepositoryLinksResource> automationRepositoryLinksRepresentationModelProcessor() {
        // This must be a class instead of a lambda so the generic parameter can be determined by spring-hateoas
        return new RepresentationModelProcessor<RepositoryLinksResource>() {
            @Override
            public RepositoryLinksResource process(RepositoryLinksResource model) {
                return model.add(
                        linkTo(methodOn(AutomationsRestController.class).getAutomations())
                                .withRel(AutomationLinkRelations.REGISTRATIONS)
                );
            }
        };
    }

}
