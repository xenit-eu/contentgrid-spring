package com.contentgrid.spring.test.fixture.invoicing.model.support;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Embeddable
@Getter
@Setter
public class Content {

    @ContentId
    @JsonIgnore
    private String id;

    @ContentLength
    @JsonProperty(access = Access.READ_ONLY)
    @CollectionFilterParam(value = "size")
    private Long length;

    @MimeType
    @CollectionFilterParam(value = "mimetype")
    private String mimetype;

    @OriginalFileName
    @CollectionFilterParam
    private String filename;
}
