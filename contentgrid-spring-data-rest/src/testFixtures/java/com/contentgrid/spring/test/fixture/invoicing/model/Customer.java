package com.contentgrid.spring.test.fixture.invoicing.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    @Column(unique=true)
    private String vat;

    @OneToMany(mappedBy = "customer")
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy="counterparty")
    private Set<Invoice> invoices = new HashSet<>();

}
