package com.contentgrid.spring.test.fixture.invoicing.model.support;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Embeddable
@NoArgsConstructor
@Getter
@Setter
public class AuditMetadata {

    @CreatedBy
    @JsonProperty(value = "created_by")
    private UserMetadata createdBy;

    @CreatedDate
    @JsonProperty(value = "created_date")
    private Instant createdDate;

    @LastModifiedBy
    @JsonProperty(value = "last_modified_by")
    private UserMetadata lastModifiedBy;

    @LastModifiedDate
    @JsonProperty(value = "last_modified_date")
    private Instant lastModifiedDate;
}
