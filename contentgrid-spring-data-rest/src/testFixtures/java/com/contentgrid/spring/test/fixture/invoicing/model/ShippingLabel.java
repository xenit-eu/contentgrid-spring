package com.contentgrid.spring.test.fixture.invoicing.model;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.Text.EqualsIgnoreCase;
import com.contentgrid.spring.test.fixture.invoicing.model.support.Content;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
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
public class ShippingLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @Column(nullable = false)
    @CollectionFilterParam(predicate = EqualsIgnoreCase.class)
    @NotNull
    private String from;

    @Column(nullable = false)
    @CollectionFilterParam(predicate = EqualsIgnoreCase.class)
    @NotNull
    private String to;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent")
    @CollectionFilterParam
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:parent")
    private ShippingLabel parent;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "barcode_picture__id"))
    @AttributeOverride(name = "length", column = @Column(name = "barcode_picture__length"))
    @AttributeOverride(name = "mimetype", column = @Column(name = "barcode_picture__mimetype"))
    @AttributeOverride(name = "filename", column = @Column(name = "barcode_picture__filename"))
    @RestResource(linkRel = "d:barcode_picture", path = "barcode_picture")
    @JsonProperty("barcode_picture")
    private Content barcodePicture;


    @Column(name = "package")
    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "package__id"))
    @AttributeOverride(name = "length", column = @Column(name = "package__length"))
    @AttributeOverride(name = "mimetype", column = @Column(name = "package__mimetype"))
    @AttributeOverride(name = "filename", column = @Column(name = "package__filename"))
    @RestResource(linkRel = "d:package", path = "package")
    @JsonProperty("package")
    private Content _package;

    public ShippingLabel(String from, String to) {
        this.from = from;
        this.to = to;
    }
}
