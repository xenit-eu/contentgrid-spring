package com.contentgrid.spring.boot.actuator.webhooks;

import com.contentgrid.spring.boot.actuator.TemplateHelper;
import java.io.FileNotFoundException;
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
    private final TemplateHelper templateHelper;

    @ReadOperation(producesFrom = WebhookConfigProducible.class)
    public String getConfig() throws IOException {
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            String contents = Files.readString(resource.getFile().toPath());
            return this.templateHelper.templateSystemAndUserVars(contents);
        }
        throw new FileNotFoundException("webhook config file at " + path + " is not present");
    }
}
