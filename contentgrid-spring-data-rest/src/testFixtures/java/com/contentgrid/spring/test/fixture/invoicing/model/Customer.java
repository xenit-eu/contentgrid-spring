package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.data.rest.validation.OnEntityDelete;
import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.contentgrid.spring.querydsl.predicate.EqualsIgnoreCase;
import com.contentgrid.spring.test.fixture.invoicing.model.support.Content;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import jakarta.validation.constraints.Size;
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
import jakarta.persistence.Version;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.content.rest.RestResource;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

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

    @Version
    private int version;

    @CreatedBy
    @JsonProperty(access = Access.READ_ONLY)
    private String createdBy;

    @CreatedDate
    @JsonProperty(access = Access.READ_ONLY)
    private Instant createdDate;

    @LastModifiedBy
    @JsonProperty(access = Access.READ_ONLY)
    private String lastModifiedBy;

    @LastModifiedDate
    @JsonProperty(access = Access.READ_ONLY)
    private Instant lastModifiedDate;

    private String name;

    @Column(unique=true, nullable = false)
    @CollectionFilterParam(predicate = EqualsIgnoreCase.class)
    @NotNull
    private String vat;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "content__id"))
    @AttributeOverride(name = "length", column = @Column(name = "content__length"))
    @AttributeOverride(name = "mimetype", column = @Column(name = "content__mimetype"))
    @AttributeOverride(name = "filename", column = @Column(name = "content__filename"))
    @RestResource(linkRel = "d:content", path = "content")
    @CollectionFilterParam
    private Content content;

    @CollectionFilterParam
    private Instant birthday;

    @JsonProperty("total_spend")
    private Integer totalSpend;

    @OneToMany(mappedBy = "customer")
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:orders")
    @CollectionFilterParam(predicate = EntityId.class, documented = false)
    private Set<Order> orders = new HashSet<>();

    @OneToMany(mappedBy="counterparty")
    @CollectionFilterParam
    @CollectionFilterParam(value = "invoices.$$id", predicate = EntityId.class, documented = false)
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:invoices")
    // Invoice#counterparty is required, this constraint ensures that there are no more invoices linked when deleting the customer
    @Size(max = 0, groups = OnEntityDelete.class)
    private Set<Invoice> invoices = new HashSet<>();

}
