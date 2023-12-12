package com.contentgrid.spring.swaggerui;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.webjars.WebJarAssetLocator;

/**
 * This controller serves the (static) Swagger UI configuration file 'swagger-initializer.js'. This static file
 * should NOT be served with standard {@link ResourceResolver} methods, because that breaks the automatic
 * version detection of {@link WebJarAssetLocator}.
 *
 */
@RestController
public class SwaggerUIInitializerController {

    private static final Resource initilizerResource = new ClassPathResource("swagger-initializer.js");

    @GetMapping(value = "/webjars/swagger-ui/swagger-initializer.js", produces = "text/javascript")
    ResponseEntity<Resource> getInitializer() {
        if (!initilizerResource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(initilizerResource);
    }

}
