package com.contentgrid.spring.test.fixture.invoicing.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String number;

    private boolean draft;

    private boolean paid;

    @ManyToOne
    @JoinColumn(name = "counterparty")
    private Customer counterparty;

    @OneToMany(mappedBy = "invoice")
    private Set<Order> orders = new HashSet<>();

    public Invoice(String number, boolean draft, boolean paid, Customer counterparty, Set<Order> orders) {
        this.number = number;
        this.draft = draft;
        this.paid = paid;

        this.counterparty = counterparty;
        if (counterparty != null) {
            counterparty.getInvoices().add(this);
        }

        this.orders = orders;
        if (orders != null) {
            orders.forEach(order -> order.setInvoice(this));
        }
    }

}