package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Property;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Value;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.rest.webmvc.RootResourceInformation;

@Value
public class DescriptionMessageSourceResolvable implements MessageSourceResolvable {
    String[] codes;

    public static DescriptionMessageSourceResolvable forEntity(RootResourceInformation information) {
        return forNestedProperty(information, List.of());
    }

    public static DescriptionMessageSourceResolvable forProperty(RootResourceInformation information, Property property) {
        return forNestedProperty(information, List.of(property));
    }

    public static DescriptionMessageSourceResolvable forNestedProperty(RootResourceInformation information, Collection<Property> properties) {
        var message = properties.stream()
                .map(Property::getName)
                .map(name -> "." + name)
                .collect(Collectors.joining());
        return new DescriptionMessageSourceResolvable(information.getResourceMetadata().getItemResourceDescription().getMessage() + message);
    }

    private DescriptionMessageSourceResolvable(String... codes) {
        this.codes = codes;
    }

    @Override
    public String getDefaultMessage() {
        return ""; // Returns null if empty string (null [default] = throws exception)
    }
}
