package com.contentgrid.spring.boot.autoconfigure.automation;

import com.contentgrid.spring.data.rest.automation.AutomationAnnotationRepresentationModelAssembler;
import com.contentgrid.spring.data.rest.automation.AutomationRepresentationModelAssembler;
import com.contentgrid.spring.data.rest.automation.AutomationsRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(AutomationsRestController.class)
@ConditionalOnResource(resources = "classpath:automation/automations.json")
@Import({
        AutomationRepresentationModelAssembler.class,
        AutomationAnnotationRepresentationModelAssembler.class
})
public class ContentGridAutomationAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    AutomationsRestController automationsRestController(AutomationRepresentationModelAssembler assembler) {
        return new AutomationsRestController(applicationContext.getResource("classpath:automation/automations.json"), assembler);
    }

}
