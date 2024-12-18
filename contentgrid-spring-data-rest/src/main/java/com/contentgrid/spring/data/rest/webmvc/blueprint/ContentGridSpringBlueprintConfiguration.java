package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.support.Repositories;
import org.springframework.hateoas.mediatype.MessageResolver;

@Configuration
public class ContentGridSpringBlueprintConfiguration {

    @Bean
    CurieProviderCustomizer dataModelCurieProvider() {
        return CurieProviderCustomizer.register(BlueprintLinkRelations.CURIE, BlueprintLinkRelations.TEMPLATE);
    }

    @Bean
    EntityRepresentationModelAssembler entityRepresentationModelAssembler(
            Repositories repositories, MessageResolver messageResolver
    ) {
        return new EntityRepresentationModelAssembler(repositories, messageResolver);
    }
}
