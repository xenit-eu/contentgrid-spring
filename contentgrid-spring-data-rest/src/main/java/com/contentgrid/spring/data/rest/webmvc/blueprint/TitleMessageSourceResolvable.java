package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Property;
import lombok.Value;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.util.TypeInformation;

@Value
public class TitleMessageSourceResolvable implements MessageSourceResolvable {
    String[] codes;

    public static TitleMessageSourceResolvable forEntity(TypeInformation<?> information) {
        return new TitleMessageSourceResolvable(information.getType().getName() + "._title");
    }

    public static TitleMessageSourceResolvable forProperty(TypeInformation<?> information, Property property) {
        var code = information.getType().getName() + "." + property.getName() + "._title";
        return new TitleMessageSourceResolvable(code);
    }

    private TitleMessageSourceResolvable(String... codes) {
        this.codes = codes;
    }

    @Override
    public String getDefaultMessage() {
        return ""; // Returns null if empty string (null [default] = throws exception)
    }
}
