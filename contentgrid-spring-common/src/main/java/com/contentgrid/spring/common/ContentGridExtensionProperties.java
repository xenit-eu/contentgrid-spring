package com.contentgrid.spring.common;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
public class ContentGridExtensionProperties {
    private Map<String, ExtensionRegistration> registration = new HashMap<>();

    public Optional<ExtensionRegistration> getRegistration(String extensionName) {
        return Optional.ofNullable(registration.get(extensionName));
    }

    @Data
    public static class ExtensionRegistration {
        private Map<String, URI> basePath = new HashMap<>();

        public Optional<URI> getBasePath(String prefixName) {
            return Optional.ofNullable(basePath.get(prefixName));
        }
    }
}
