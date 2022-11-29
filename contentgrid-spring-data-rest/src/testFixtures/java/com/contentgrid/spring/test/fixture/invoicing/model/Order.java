package com.contentgrid.spring.test.fixture.invoicing.model;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "customer")
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "invoice", foreignKey = @ForeignKey(foreignKeyDefinition = "foreign key (\"invoice\") references \"invoice\" ON DELETE set NULL"))
    private Invoice invoice;

    public Order(Customer customer) {

        if (customer != null) {
            this.customer = customer;
            var orders = customer.getOrders();
            if (orders != null) {
                orders.add(this);
            }
        }
    }
}
