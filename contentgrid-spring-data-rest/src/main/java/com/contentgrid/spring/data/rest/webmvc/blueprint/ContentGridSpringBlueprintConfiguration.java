package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentGridSpringBlueprintConfiguration {

    @Bean
    CurieProviderCustomizer dataModelCurieProvider() {
        return CurieProviderCustomizer.register(BlueprintLinkRelations.CURIE, BlueprintLinkRelations.TEMPLATE);
    }
}
