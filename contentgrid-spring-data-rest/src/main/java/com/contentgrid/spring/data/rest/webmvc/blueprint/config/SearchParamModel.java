package com.contentgrid.spring.data.rest.webmvc.blueprint.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SearchParamModel {

    private String name;
    private String title;
    private String type;
}
