package com.contentgrid.spring.test.fixture.invoicing.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    private String street;
    private String zip;
    private String city;

    @OneToOne(mappedBy = "shippingAddress")
    private Order order;

    public ShippingAddress(String street, String zip, String city) {
        this.street = street;
        this.zip = zip;
        this.city = city;
    }
}
