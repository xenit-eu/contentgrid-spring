package com.contentgrid.spring.boot.actuator.policy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.Resource;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.SystemPropertyUtils;

@WebEndpoint(id = "policy")
@RequiredArgsConstructor
public class PolicyActuator {
    private final Resource policyResource;
    private final PolicyVariables policyVariables;
    private static final PropertyPlaceholderHelper PROPERTY_PLACEHOLDER_HELPER = new PropertyPlaceholderHelper(
            SystemPropertyUtils.PLACEHOLDER_PREFIX,
            SystemPropertyUtils.PLACEHOLDER_SUFFIX
    );

    @ReadOperation(producesFrom = RegoProducible.class)
    public String readPolicy() throws IOException {
        if (policyResource.exists()) {
            String contents = Files.readString(policyResource.getFile().toPath());

            return PROPERTY_PLACEHOLDER_HELPER.replacePlaceholders(contents, policyVariables);
        } else {
            throw new FileNotFoundException("rego file at " + policyResource.getDescription() + " is not present");
        }
    }
}
