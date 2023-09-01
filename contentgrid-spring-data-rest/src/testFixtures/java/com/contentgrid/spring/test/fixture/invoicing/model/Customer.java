package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.querydsl.predicate.EqualsIgnoreCase;
import com.contentgrid.spring.test.fixture.invoicing.model.support.Content;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
    @CollectionFilterParam(predicate = EqualsIgnoreCase.class)
    private String vat;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "content__id"))
    @AttributeOverride(name = "length", column = @Column(name = "content__length"))
    @AttributeOverride(name = "mimetype", column = @Column(name = "content__mimetype"))
    @AttributeOverride(name = "filename", column = @Column(name = "content__filename"))
    @RestResource(linkRel = "d:content", path = "content")
    @CollectionFilterParam
    private Content content;

    @OneToMany(mappedBy = "customer")
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:orders")
    @CollectionFilterParam(predicate = EntityId.class, documented = false)
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy="counterparty")
    @CollectionFilterParam
    @CollectionFilterParam(value = "invoices.$$id", predicate = EntityId.class, documented = false)
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:invoices")
    private Set<Invoice> invoices = new HashSet<>();

}
