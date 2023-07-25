package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotations.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.None;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
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
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

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
    private Customer counterparty;

    @OneToMany(mappedBy = "invoice")
    @CollectionFilterParam
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