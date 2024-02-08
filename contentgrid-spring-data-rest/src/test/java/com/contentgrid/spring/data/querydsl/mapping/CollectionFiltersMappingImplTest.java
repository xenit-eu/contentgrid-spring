package com.contentgrid.spring.data.querydsl.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import com.contentgrid.spring.data.querydsl.mapping.CollectionFiltersMappingImplTest.LocalConfiguration;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.QCustomer;
import com.contentgrid.spring.test.fixture.invoicing.model.QInvoice;
import com.contentgrid.spring.test.fixture.invoicing.model.QOrder;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = {
        InvoicingApplication.class,
        LocalConfiguration.class
})
class CollectionFiltersMappingImplTest {
    @Autowired
    CollectionFiltersMapping collectionFiltersMapping;

    @Test
    void forDomainType() {
        assertThat(collectionFiltersMapping.forDomainType(Customer.class).filters())
                .map(CollectionFilter::getFilterName)
                .containsExactlyInAnyOrder(
                        "vat",
                        "content.size",
                        "content.mimetype",
                        "content.filename",
                        "birthday",
                        "invoices.$$id",
                        "invoices.number",
                        "invoices.paid",
                        "invoices.content.length",
                        "invoices.orders.id",
                        "invoices.counterparty",
                        "orders"
                        // not included because predicate is None
                        // "invoices.content.length.lt",
                        // "invoices.content.length.gt"
                );
    }

    @Test
    void forProperty() {
        assertThat(collectionFiltersMapping.forProperty(Customer.class, "vat"))
                .hasValueSatisfying(filter -> {
                    assertThat(filter.getFilterName()).isEqualTo("vat");
                    assertThat(filter.getPath()).isEqualTo(QCustomer.customer.vat);
                    assertThat(filter.isDocumented()).isTrue();
                });

        assertThat(collectionFiltersMapping.forProperty(Customer.class, "content", "length"))
                .hasValueSatisfying(filter -> {
                    assertThat(filter.getFilterName()).isEqualTo("content.size");
                    assertThat(filter.getPath()).isEqualTo(QCustomer.customer.content.length);
                    assertThat(filter.isDocumented()).isTrue();
                });

        assertThat(collectionFiltersMapping.forProperty(Customer.class, "content")).isEmpty();

        assertThatThrownBy(() -> collectionFiltersMapping.forProperty(Customer.class, "content", "size"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Path 'customer.content' does not have property 'size'");

    }

    @Test
    void forIdProperty() {
        assertThat(collectionFiltersMapping.forIdProperty(Customer.class, "invoices")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("invoices.$$id");
            assertThat(filter.getPath()).isEqualTo(QCustomer.customer.invoices.any().id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Order.class, "customer")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("customer._id");
            assertThat(filter.getPath()).isEqualTo(QOrder.order.customer.id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Invoice.class, "counterparty")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("counterparty");
            assertThat(filter.getPath()).isEqualTo(QInvoice.invoice.counterparty.id);
            assertThat(filter.isDocumented()).isFalse();
        });

        assertThat(collectionFiltersMapping.forIdProperty(Invoice.class, "orders")).hasValueSatisfying(filter -> {
            assertThat(filter.getFilterName()).isEqualTo("orders.id");
            assertThat(filter.getPath()).isEqualTo(QInvoice.invoice.orders.any().id);
            assertThat(filter.isDocumented()).isTrue();
        });

        assertThat(collectionFiltersMapping.forIdProperty(ShippingAddress.class, "order")).isEmpty();
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = {CollectionFiltersMappingImplTest.class, InvoicingApplication.class})
    @EnableJpaRepositories(basePackageClasses = {CollectionFiltersMappingImplTest.class, InvoicingApplication.class}, considerNestedRepositories = true)
    public static class LocalConfiguration {

    }

    @Entity
    public static class EntityWithRenamedId {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID renamedId;

        @OneToOne
        @CollectionFilterParam(value = "order$id", predicate = EntityId.class)
        private Order order;
    }

    @RepositoryRestResource
    public interface EntityWithRenamedIdRepository extends JpaRepository<EntityWithRenamedId, UUID>, QuerydslPredicateExecutor<EntityWithRenamedId> {}

}
