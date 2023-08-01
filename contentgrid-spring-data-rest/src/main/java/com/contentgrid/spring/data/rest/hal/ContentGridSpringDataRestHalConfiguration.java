package com.contentgrid.spring.data.rest.hal;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.mediatype.hal.CurieProvider;

@Configuration(proxyBeanMethods = false)
public class ContentGridSpringDataRestHalConfiguration {

    @Bean
    CurieProvider contentGridCurieProvider(ObjectProvider<CurieProviderCustomizer> customizers) {
        CurieProviderBuilder curieProvider = new ContentGridCurieProvider();

        for (CurieProviderCustomizer customizer : customizers) {
            curieProvider = customizer.customize(curieProvider);
        }

        return curieProvider.build();
    }

}
