package com.contentgrid.spring.data.rest.links;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.data.rest.links.ContentGridSpringDataLinksConfigurationTest.TestConfig;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
        TestConfig.class
})
class ContentGridSpringDataLinksConfigurationTest {
    @Autowired
    LinkCollector linkCollector;

    private static final LinkRelation CUSTOM_REL = HalLinkRelation.uncuried("https://example.com/rels/custom");

    @Configuration(proxyBeanMethods = false)
    public static class TestConfig {
        @Bean
        ContentGridLinkCollector<Customer> customerCustomLinks() {
            return (customer, links) -> {
                // Locating the existing content link is part of the test:
                // this link collector should be ordered after the contentgrid-spring provided ones
                var contentLink = links.stream()
                        .filter(link -> link.hasRel(ContentGridLinkRelations.CONTENT) && Objects.equals(link.getName(), "content"))
                        .findFirst()
                        .orElseThrow();

                return links.and(contentLink.withRel(CUSTOM_REL));
            };
        }

    }

    @Test
    void classSpecificLinkCollector() {
        var customer = new Customer();
        customer.setId(UUID.randomUUID());

        var customerLinks = linkCollector.getLinksFor(customer);

        assertThat(customerLinks.toList())
                .satisfiesOnlyOnce(link -> {
                    assertThat(link.getRel()).isEqualTo(CUSTOM_REL);
                });

        var order = new Order();
        order.setId(UUID.randomUUID());

        var orderLinks = linkCollector.getLinksFor(order);

        assertThat(orderLinks.toList())
                .noneSatisfy(link -> {
                    assertThat(link.getRel()).isEqualTo(CUSTOM_REL);
                });
    }


}
