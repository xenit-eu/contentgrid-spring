package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.test.fixture.invoicing.model.support.Content;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.content.rest.RestResource;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    private String name;

    @Column(unique=true, nullable = false)
    @CollectionFilterParam
    private String vat;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "content__id"))
    @AttributeOverride(name = "length", column = @Column(name = "content__length"))
    @AttributeOverride(name = "mimetype", column = @Column(name = "content__mimetype"))
    @AttributeOverride(name = "filename", column = @Column(name = "content__filename"))
    @RestResource(linkRel = "content", path = "content")
    @CollectionFilterParam
    private Content content;

    @OneToMany(mappedBy = "customer")
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy="counterparty")
    @CollectionFilterParam
    private Set<Invoice> invoices = new HashSet<>();

}
