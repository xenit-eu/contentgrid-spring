package com.contentgrid.spring.boot.autoconfigure.automation;

import com.contentgrid.automations.ContentGridAutomationsConfiguration;
import com.contentgrid.spring.boot.autoconfigure.data.web.ContentGridSpringDataRestAutoConfiguration;
import com.contentgrid.thunx.predicates.model.ThunkExpression;
import com.contentgrid.thunx.spring.data.context.AbacContextSupplier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;

@AutoConfiguration(
        after = ContentGridSpringDataRestAutoConfiguration.class
)
@ConditionalOnWebApplication
@ConditionalOnClass({ContentGridAutomationsConfiguration.class, AbacContextSupplier.class, ThunkExpression.class})
@Import(ContentGridAutomationsConfiguration.class)
public class ContentGridAutomationAutoConfiguration {

}
