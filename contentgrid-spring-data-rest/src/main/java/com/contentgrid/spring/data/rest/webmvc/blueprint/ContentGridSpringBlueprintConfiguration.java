package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        EntityProfileRepresentationModelAssembler.class,
        RelationProfileRepresentationModelAssembler.class
})
public class ContentGridSpringBlueprintConfiguration {

    @Bean
    CurieProviderCustomizer dataModelCurieProvider() {
        return CurieProviderCustomizer.register(BlueprintLinkRelations.CURIE, BlueprintLinkRelations.TEMPLATE);
    }
}
