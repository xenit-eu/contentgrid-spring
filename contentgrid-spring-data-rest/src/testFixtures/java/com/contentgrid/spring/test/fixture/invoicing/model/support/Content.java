package com.contentgrid.spring.test.fixture.invoicing.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import javax.persistence.Embeddable;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Embeddable
public class Content {

    @ContentId
    @JsonIgnore
    private String id;

    @ContentLength
    @JsonProperty(access = Access.READ_ONLY)
    private Long length;

    @MimeType
    private String mimetype;

    @OriginalFileName
    private String filename;
}
