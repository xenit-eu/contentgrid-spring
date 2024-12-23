package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Property;
import lombok.Value;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.StringUtils;

@Value
public class DescriptionMessageSourceResolvable implements MessageSourceResolvable {
    String[] codes;

    public static DescriptionMessageSourceResolvable forEntity(RootResourceInformation information) {
        // e.g. "rest.description.d\:person"
        return new DescriptionMessageSourceResolvable(information.getResourceMetadata().getItemResourceDescription().getMessage());
    }

    public static DescriptionMessageSourceResolvable forProperty(RootResourceInformation information, Property property) {
        // e.g. "rest.description.d\:person.firstName"
        return new DescriptionMessageSourceResolvable(information.getResourceMetadata().getItemResourceDescription().getMessage() + "." + property.getName());
    }

    public static DescriptionMessageSourceResolvable forNestedProperty(TypeInformation<?> information, Property property) {
        // e.g. "rest.description.auditMetadata.createdDate"
        var className = StringUtils.uncapitalize(information.getType().getSimpleName());
        var code = "rest.description.%s.%s".formatted(className, property.getName());
        return new DescriptionMessageSourceResolvable(code);
    }

    private DescriptionMessageSourceResolvable(String... codes) {
        this.codes = codes;
    }

    @Override
    public String getDefaultMessage() {
        return ""; // Returns null if empty string (null [default] = throws exception)
    }
}
