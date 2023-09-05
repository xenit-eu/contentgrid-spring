package com.contentgrid.spring.data.rest.links;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.hateoas.AffordanceModel;
import org.springframework.hateoas.AffordanceModel.InputPayloadMetadata;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.http.HttpMethod;
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
                .satisfiesExactly(link -> {
                    assertThat(link.getName()).isEqualTo("order");
                    assertThat(link.getHref()).isEqualTo("http://localhost/shipping-addresses/%s/order".formatted(entity.getId()));

                    assertThat(link.getAffordances()).<AffordanceModel>map(affordance -> affordance.getAffordanceModel(MediaTypes.HAL_FORMS_JSON))
                            .satisfiesExactlyInAnyOrder(
                                    setOrderAffordance -> {
                                        assertThat(setOrderAffordance.getName()).isEqualTo("set-order");
                                        assertThat(setOrderAffordance.getHttpMethod()).isEqualTo(HttpMethod.PUT);
                                        assertThat(setOrderAffordance.getLink().getHref()).isEqualTo(link.getHref());
                                        assertThat(setOrderAffordance.getInput().getPrimaryMediaType()).isEqualTo(RestMediaTypes.TEXT_URI_LIST);
                                        assertThat(setOrderAffordance.getInput().getType()).isEqualTo(ShippingAddress.class);
                                        assertThat(setOrderAffordance.getInput().stream()).satisfiesExactly(orderRelation -> {
                                            assertThat(orderRelation.getName()).isEqualTo("order");
                                            assertThat(orderRelation.getInputType()).isEqualTo(HtmlInputType.URL_VALUE);
                                        });
                                    },
                                    clearOrderAffordance -> {
                                        assertThat(clearOrderAffordance.getName()).isEqualTo("clear-order");
                                        assertThat(clearOrderAffordance.getHttpMethod()).isEqualTo(HttpMethod.DELETE);
                                        assertThat(clearOrderAffordance.getLink().getHref()).isEqualTo(link.getHref());
                                        assertThat(clearOrderAffordance.getInput()).isEqualTo(InputPayloadMetadata.NONE);
                                    }
                            );
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

                            assertThat(link.getAffordances()).<AffordanceModel>map(affordance -> affordance.getAffordanceModel(MediaTypes.HAL_FORMS_JSON))
                                    .satisfiesExactly(
                                            addOrdersAffordance -> {
                                                assertThat(addOrdersAffordance.getName()).isEqualTo("add-orders");
                                                assertThat(addOrdersAffordance.getHttpMethod()).isEqualTo(HttpMethod.POST);
                                                assertThat(addOrdersAffordance.getLink().getHref()).isEqualTo(link.getHref());
                                                assertThat(addOrdersAffordance.getInput().getPrimaryMediaType()).isEqualTo(RestMediaTypes.TEXT_URI_LIST);
                                                assertThat(addOrdersAffordance.getInput().getType()).isEqualTo(Customer.class);
                                                assertThat(addOrdersAffordance.getInput().stream()).satisfiesExactly(orderRelation -> {
                                                    assertThat(orderRelation.getName()).isEqualTo("orders");
                                                    assertThat(orderRelation.getInputType()).isEqualTo(HtmlInputType.URL_VALUE);
                                                });
                                            }
                                    );
                        },
                        link -> {
                            assertThat(link.getName()).isEqualTo("invoices");
                            assertThat(link.getHref()).isEqualTo("http://localhost/customers/%s/invoices".formatted(entity.getId()));

                            assertThat(link.getAffordances()).<AffordanceModel>map(affordance -> affordance.getAffordanceModel(MediaTypes.HAL_FORMS_JSON))
                                    .satisfiesExactly(
                                            addInvoicesAffordance -> {
                                                assertThat(addInvoicesAffordance.getName()).isEqualTo("add-invoices");
                                                assertThat(addInvoicesAffordance.getHttpMethod()).isEqualTo(HttpMethod.POST);
                                                assertThat(addInvoicesAffordance.getLink().getHref()).isEqualTo(link.getHref());
                                                assertThat(addInvoicesAffordance.getInput().getPrimaryMediaType()).isEqualTo(RestMediaTypes.TEXT_URI_LIST);
                                                assertThat(addInvoicesAffordance.getInput().getType()).isEqualTo(Customer.class);
                                                assertThat(addInvoicesAffordance.getInput().stream()).satisfiesExactly(orderRelation -> {
                                                    assertThat(orderRelation.getName()).isEqualTo("invoices");
                                                    assertThat(orderRelation.getInputType()).isEqualTo(HtmlInputType.URL_VALUE);
                                                });
                                            }
                                    );
                        }
                );
    }

}