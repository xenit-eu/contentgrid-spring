package com.contentgrid.spring.boot.autoconfigure.automation;

import com.contentgrid.automations.rest.AutomationAnnotationRepresentationModelAssembler;
import com.contentgrid.automations.rest.AutomationRepresentationModelAssembler;
import com.contentgrid.automations.rest.AutomationsRestController;
import com.contentgrid.thunx.predicates.model.ThunkExpression;
import com.contentgrid.thunx.spring.data.context.AbacContextSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({ AutomationsRestController.class, AbacContextSupplier.class, ThunkExpression.class })
@Import({
        AutomationRepresentationModelAssembler.class,
        AutomationAnnotationRepresentationModelAssembler.class
})
public class ContentGridAutomationAutoConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    AutomationsRestController automationsRestController(AutomationRepresentationModelAssembler assembler,
            AbacContextSupplier abacContextSupplier) {
        return new AutomationsRestController(applicationContext.getResource("classpath:automation/automations.json"),
                assembler, abacContextSupplier);
    }

}
