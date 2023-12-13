package com.contentgrid.spring.swagger.ui;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.webjars.WebJarAssetLocator;

/**
 * This controller serves the (static) Swagger UI configuration file 'swagger-initializer.js'.
 *
 * <p>
 * This static file should NOT be served with standard {@link ResourceResolver} methods:
 * <ul>
 *     <li>If served via {@code /META-INF/resources/webjars/swagger-ui/}, the automatic version detection of
 *     {@link WebJarAssetLocator} breaks.</li>
 *     <li>If served via {@code /META-INF/resources/webjars/swagger-ui/<version-number>/}, (automated) dependency
 *     updates would break the configuration.</li>
 * </ul>
 * </p>
 */
@RestController
public class SwaggerUIInitializerController {

    private static final Resource initializerResource = new ClassPathResource("swagger-initializer.js");

    @GetMapping(value = "/webjars/swagger-ui/swagger-initializer.js", produces = "text/javascript")
    ResponseEntity<Resource> getInitializer() {
        if (!initializerResource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(initializerResource);
    }

}
