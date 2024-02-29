package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.test.fixture.invoicing.model.support.AuditMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EntityListeners;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.rest.core.annotation.RestResource;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @Version
    private int version;

    @JsonProperty(value = "audit_metadata", access = Access.READ_ONLY)
    @Embedded
    @AttributeOverride(name = "createdBy.id", column = @Column(name = "audit_metadata__created_by__id"))
    @AttributeOverride(name = "createdBy.name", column = @Column(name = "audit_metadata__created_by__name"))
    @AttributeOverride(name = "createdDate", column = @Column(name = "audit_metadata__created_date"))
    @AttributeOverride(name = "lastModifiedBy.id", column = @Column(name = "audit_metadata__last_modified_by__id"))
    @AttributeOverride(name = "lastModifiedBy.name", column = @Column(name = "audit_metadata__last_modified_by__name"))
    @AttributeOverride(name = "lastModifiedDate", column = @Column(name = "audit_metadata__last_modified_date"))
    private AuditMetadata auditMetadata = new AuditMetadata();

    @ManyToOne
    @JoinColumn(name = "customer")
    @CollectionFilterParam
    @CollectionFilterParam(value = "customer._id", predicate = EntityId.class, documented = false)
    @RestResource(rel = "d:customer")
    private Customer customer;

    @Column(name = "__invoice_id__orders", insertable = false, updatable = false)
    @CollectionFilterParam(value = "invoice._id", documented = false)
    @JsonIgnore
    private UUID __internal_invoice_id;

    @ManyToMany
    @RestResource(rel = "d:promos")
    @CollectionFilterParam(value = "promos._id", predicate = EntityId.class, documented = false)
    private Set<PromotionCampaign> promos = new HashSet<>();

    @OneToOne
    @JsonProperty("shipping_address")
    @CollectionFilterParam("shipping_address")
    @CollectionFilterParam(value = "shipping_address._id", predicate = EntityId.class, documented = false)
    @RestResource(rel = "d:shipping-address")
    private ShippingAddress shippingAddress;

    public Order(Customer customer) {
        if (customer != null) {
            this.customer = customer;
            var orders = customer.getOrders();
            if (orders != null) {
                orders.add(this);
            }
        }
    }

    public Order(Customer customer, ShippingAddress address, Set<PromotionCampaign> promos) {
        this(customer);

        this.shippingAddress = address;
        address.setOrder(this);
        promos.forEach(this::addPromo);
    }

    public void addPromo(@NonNull PromotionCampaign promo) {
        if (this.promos.add(promo)) {
            promo.addOrder(this);
        }
    }
}
