package com.contentgrid.spring.boot.actuator.webhooks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

@WebEndpoint(id = "webhooks")
@RequiredArgsConstructor
public class WebHooksConfigActuator {
    private final Resource webhookResource;
    private final WebhookVariables webhookVariables;
    private static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper(
            SystemPropertyUtils.PLACEHOLDER_PREFIX,
            SystemPropertyUtils.PLACEHOLDER_SUFFIX
    );

    @ReadOperation(producesFrom = WebhookConfigProducible.class)
    public String getConfig() throws IOException {
        if (webhookResource.exists()) {
            String contents = Files.readString(webhookResource.getFile().toPath());
            return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(contents, webhookVariables);
        }
        throw new FileNotFoundException("webhook config file at " + webhookResource.getDescription() + " is not present");
    }
}
