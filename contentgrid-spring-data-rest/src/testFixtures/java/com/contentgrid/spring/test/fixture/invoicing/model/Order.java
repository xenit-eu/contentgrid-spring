package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.rest.core.annotation.RestResource;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer")
    @CollectionFilterParam
    @CollectionFilterParam(value = "customer._id", predicate = EntityId.class, documented = false)
    @RestResource(rel = "d:customer")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "invoice", foreignKey = @ForeignKey(foreignKeyDefinition = "foreign key (\"invoice\") references \"invoice\" ON DELETE set NULL"))
    @CollectionFilterParam(value = "invoice._id", predicate = EntityId.class, documented = false)
    @RestResource(exported = false)
    private Invoice invoice;

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
