package com.contentgrid.spring.boot.actuator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;

@UtilityClass
public class Utils {

    public String readContents(Resource resource) throws IOException {
        try(InputStream resourceStream = resource.getInputStream()) {
            return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
