package com.contentgrid.spring.boot.actuator.webhooks;

import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

@WebEndpoint(id = "webhooks")
@RequiredArgsConstructor
public class WebHooksConfigActuator {
    private final String path; // webhooks/config.json
    private final WebhooksTemplatingProperties properties;
    private final PropertyPlaceholderHelper helper;

    @ReadOperation(producesFrom = WebhookConfigProducible.class)
    public String getConfig() throws IOException {
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            String contents = Files.readString(resource.getFile().toPath());
            return this.helper.replacePlaceholders(contents, (property) -> properties.getVariables().get(property));
        }
        throw new IllegalStateException("webhook config file at " + path + " is not present");
    }
}
