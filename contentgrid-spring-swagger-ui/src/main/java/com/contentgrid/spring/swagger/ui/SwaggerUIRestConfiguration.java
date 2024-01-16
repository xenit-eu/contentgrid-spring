package com.contentgrid.spring.swagger.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(SwaggerUIInitializerController.class)
public class SwaggerUIRestConfiguration {

}
