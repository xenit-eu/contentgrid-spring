package com.contentgrid.spring.boot.actuator.policy;

import com.contentgrid.spring.boot.actuator.TemplateHelper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;

@WebEndpoint(id = "policy")
@RequiredArgsConstructor
public class PolicyActuator {
    private final String path; // rego/policy.rego
    private final TemplateHelper templateHelper;

    @ReadOperation(producesFrom = RegoProducible.class)
    public String readPolicy() throws IOException {
        Resource resource = new ClassPathResource(path);
        if (resource.exists()) {
            String contents = Files.readString(resource.getFile().toPath());
            return this.templateHelper.templateSystemVars(contents);
        } else {
            throw new FileNotFoundException("rego file at " + path + " is not present");
        }
    }
}
