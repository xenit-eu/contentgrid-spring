package com.contentgrid.spring.data.support.auditing.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserMetadata {
    @JsonIgnore
    private String id;

    @JsonIgnore
    private String namespace;

    @JsonValue
    @JsonProperty(access = Access.READ_ONLY)
    private String name;
}
