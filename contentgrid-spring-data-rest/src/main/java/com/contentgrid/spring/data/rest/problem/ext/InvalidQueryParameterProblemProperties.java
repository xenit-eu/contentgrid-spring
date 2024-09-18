package com.contentgrid.spring.data.rest.problem.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
public class InvalidQueryParameterProblemProperties {

    @JsonProperty("query_parameter")
    String queryParameter;

    @JsonProperty("invalid_value")
    String invalidValue;
}
