package com.contentgrid.spring.data.rest.hal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.contentgrid.spring.data.rest.hal.ContentGridSpringDataRestHalConfigurationTest.CurieProviderCustomizers;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        ContentGridSpringDataRestHalConfiguration.class,
        CurieProviderCustomizers.class
})
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ContentGridSpringDataRestHalConfigurationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class CurieProviderCustomizers {
        @Bean
        CurieProviderCustomizer curieProviderTestCustomizer() {
            return builder -> builder.withCurie("test", UriTemplate.of("https://example.com/rels/{x}"));
        }

        @Bean
        CurieProviderCustomizer curieProviderExtCustomizer() {
            return builder -> builder.withCurie("ext", UriTemplate.of("https://ext.invalid/{rel}"));
        }
    }

    @Test
    void customizersApplied(ApplicationContext context) {
        var curies = context.getBean(CurieProvider.class).getCurieInformation(Links.NONE);

        assertThat(curies).asInstanceOf(InstanceOfAssertFactories.list(Link.class)).containsExactlyInAnyOrder(
                Link.of("https://example.com/rels/{x}", HalLinkRelation.CURIES).withName("test"),
                Link.of("https://ext.invalid/{rel}", HalLinkRelation.CURIES).withName("ext")
        );
    }


}