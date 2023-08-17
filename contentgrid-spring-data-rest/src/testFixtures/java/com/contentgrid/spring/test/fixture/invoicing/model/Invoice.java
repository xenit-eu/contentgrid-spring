package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.None;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.rest.RestResource;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @Column(nullable = false)
    @CollectionFilterParam
    private String number;

    private boolean draft;

    @CollectionFilterParam
    private boolean paid;

    @ContentId
    @JsonIgnore
    @RestResource(linkRel = "d:content")
    private String contentId;

    @ContentLength
    @JsonProperty(value = "content_length", access = Access.READ_ONLY)
    @CollectionFilterParam("content.length")
    @CollectionFilterParam(value = "content.length.lt", predicate = None.class)
    @CollectionFilterParam(value = "content.length.gt", predicate = None.class)
    private Long contentLength;

    @MimeType
    @JsonProperty("content_mimetype")
    private String contentMimetype;

    @OriginalFileName
    @JsonProperty("content_filename")
    private String contentFilename;

    @ContentId
    @JsonIgnore
    @RestResource(linkRel = "d:attachment")
    private String attachmentId;

    @ContentLength
    @JsonProperty(value = "attachment_length", access = Access.READ_ONLY)
    private Long attachmentLength;

    @MimeType
    @JsonProperty("attachment_mimetype")
    private String attachmentMimetype;

    @OriginalFileName
    @JsonProperty("attachment_filename")
    private String attachmentFilename;

    @ManyToOne(optional = false)
    @JoinColumn(name = "counterparty", nullable = false)
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:counterparty")
    private Customer counterparty;

    @OneToMany(mappedBy = "invoice")
    @CollectionFilterParam
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:orders")
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