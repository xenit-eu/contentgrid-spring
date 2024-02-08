package com.contentgrid.spring.data.rest.problem.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class InvalidFilterProblemProperties {

    String property;

    @JsonProperty("invalid_value")
    Object invalidValue;
}
