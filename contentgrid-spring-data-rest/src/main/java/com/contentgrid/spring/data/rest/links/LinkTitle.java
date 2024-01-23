package com.contentgrid.spring.data.rest.links;

import lombok.Value;
import org.springframework.context.MessageSourceResolvable;

@Value
class LinkTitle implements MessageSourceResolvable {
    String[] codes;

    public static LinkTitle forEntity(Class<?> domainType) {
        return new LinkTitle(domainType.getName() + "._title");
    }

    public static LinkTitle forProperty(Class<?> domainType, String propertyName) {
        return new LinkTitle(domainType.getName() + "." + propertyName + "._title");
    }

    private LinkTitle(String... codes) {
        this.codes = codes;
    }

    @Override
    public String getDefaultMessage() {
        return "";
    }
}
