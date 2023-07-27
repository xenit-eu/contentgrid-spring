package com.contentgrid.spring.data.rest.affordances;

import com.contentgrid.spring.data.rest.mapping.ContentGridDomainTypeMappingConfiguration;
import com.contentgrid.spring.data.rest.webmvc.DomainTypeToHalFormsPayloadMetadataConverter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.core.support.SelfLinkProvider;

@Configuration(proxyBeanMethods = false)
@Import(ContentGridDomainTypeMappingConfiguration.class)
public class ContentGridSpringDataRestAffordancesConfiguration {
    @Bean
    public BeanPostProcessor replaceSelfLinkProviderWithAffordanceInjectingSelfLinkProvider(ObjectProvider<DomainTypeToHalFormsPayloadMetadataConverter> payloadMetadataConverter) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if(bean instanceof SelfLinkProvider selfLinkProvider) {
                    return new AffordanceInjectingSelfLinkProvider(
                            selfLinkProvider,
                            payloadMetadataConverter
                    );
                }

                return bean;
            }
        };
    }

    @Bean
    AffordanceCollectionRepresentationModelProcessor affordanceCollectionRepresentationModelProcessor() {
        return new AffordanceCollectionRepresentationModelProcessor();
    }

}
