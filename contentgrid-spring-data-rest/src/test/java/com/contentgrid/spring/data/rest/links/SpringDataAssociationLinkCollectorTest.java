package com.contentgrid.spring.data.rest.links;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.hateoas.Links;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class
})
class SpringDataAssociationLinkCollectorTest {
    @Autowired
    SpringDataAssociationLinkCollector associationLinkCollector;

    @Test
    void associationLinksForToOne() {
        var entity = new ShippingAddress();
        entity.setId(UUID.randomUUID());

        assertThat(associationLinkCollector.getLinksFor(entity, Links.NONE))
                .allSatisfy(link -> {
                    assertThat(link.getRel()).isEqualTo(ContentGridLinkRelations.RELATION);
                })
                .satisfiesExactlyInAnyOrder(link -> {
                    assertThat(link.getName()).isEqualTo("order");
                    assertThat(link.getHref()).isEqualTo("http://localhost/shipping-addresses/%s/order".formatted(entity.getId()));
                });
    }

    @Test
    void associationLinksForToMany() {
        var entity = new Customer();
        entity.setId(UUID.randomUUID());

        assertThat(associationLinkCollector.getLinksFor(entity, Links.NONE))
                .allSatisfy(link -> {
                    assertThat(link.getRel()).isEqualTo(ContentGridLinkRelations.RELATION);
                })
                .satisfiesExactlyInAnyOrder(
                        link -> {
                            assertThat(link.getName()).isEqualTo("orders");
                            assertThat(link.getHref()).isEqualTo("http://localhost/customers/%s/orders".formatted(entity.getId()));
                        },
                        link -> {
                            assertThat(link.getName()).isEqualTo("invoices");
                            assertThat(link.getHref()).isEqualTo("http://localhost/customers/%s/invoices".formatted(entity.getId()));
                        }
                );
    }

}