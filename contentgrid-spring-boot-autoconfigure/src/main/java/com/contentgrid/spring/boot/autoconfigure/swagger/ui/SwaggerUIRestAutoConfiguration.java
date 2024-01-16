package com.contentgrid.spring.boot.autoconfigure.swagger.ui;

import com.contentgrid.spring.swagger.ui.SwaggerUIRestConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(SwaggerUIRestConfiguration.class)
@Import(SwaggerUIRestConfiguration.class)
public class SwaggerUIRestAutoConfiguration {

}
