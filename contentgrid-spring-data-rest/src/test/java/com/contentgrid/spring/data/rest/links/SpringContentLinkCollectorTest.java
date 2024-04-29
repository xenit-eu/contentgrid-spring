package com.contentgrid.spring.data.rest.links;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
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
class SpringContentLinkCollectorTest {
    @Autowired
    SpringContentLinkCollector linkCollector;

    @Test
    void contentLinksForDirect() {
        var entity = new Invoice();
        entity.setId(UUID.randomUUID());

        assertThat(linkCollector.getLinksFor(entity, Links.NONE))
                .allSatisfy(link -> {
                    assertThat(link.getRel()).isEqualTo(ContentGridLinkRelations.CONTENT);
                })
                .satisfiesExactlyInAnyOrder(
                        link -> {
                            assertThat(link.getName()).isEqualTo("content");
                            assertThat(link.getHref()).isEqualTo("http://localhost/invoices/%s/content".formatted(entity.getId()));
                        },
                        link -> {
                            assertThat(link.getName()).isEqualTo("attached_document");
                            assertThat(link.getHref()).isEqualTo("http://localhost/invoices/%s/attached-document".formatted(entity.getId()));
                        }
                );
    }

    @Test
    void contentLinksForEmbedded() {
        var entity = new Customer();
        entity.setId(UUID.randomUUID());

        assertThat(linkCollector.getLinksFor(entity, Links.NONE))
                .allSatisfy(link -> {
                    assertThat(link.getRel()).isEqualTo(ContentGridLinkRelations.CONTENT);
                })
                .satisfiesExactlyInAnyOrder(
                        link -> {
                            assertThat(link.getName()).isEqualTo("content");
                            assertThat(link.getHref()).isEqualTo("http://localhost/customers/%s/content".formatted(entity.getId()));
                        }
                );
    }

}