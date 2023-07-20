package com.contentgrid.spring.test.fixture.invoicing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

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
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "invoice", foreignKey = @ForeignKey(foreignKeyDefinition = "foreign key (\"invoice\") references \"invoice\" ON DELETE set NULL"))
    private Invoice invoice;

    @ManyToMany
    private Set<PromotionCampaign> promos = new HashSet<>();

    @OneToOne
    @JsonProperty("shipping_address")
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
