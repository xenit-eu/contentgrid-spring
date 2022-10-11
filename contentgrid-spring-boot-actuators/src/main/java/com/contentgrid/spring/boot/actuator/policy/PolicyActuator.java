package com.contentgrid.spring.boot.actuator.policy;

import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

@WebEndpoint(id = "policy")
@RequiredArgsConstructor
public class PolicyActuator {
    private final String path; // rego/policy.rego
    private final PolicyTemplatingProperties properties;
    private final PropertyPlaceholderHelper helper;

    @ReadOperation(producesFrom = RegoProducible.class)
    public String readPolicy() throws IOException {
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            String contents = Files.readString(resource.getFile().toPath());
            return this.helper.replacePlaceholders(contents, (property) -> properties.getPolicy().get(property));
        } else {
            throw new IllegalStateException("rego file at " + path + " is not present");
        }
    }
}
