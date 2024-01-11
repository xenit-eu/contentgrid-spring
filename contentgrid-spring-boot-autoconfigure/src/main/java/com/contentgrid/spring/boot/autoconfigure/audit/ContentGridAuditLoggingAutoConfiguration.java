package com.contentgrid.spring.boot.autoconfigure.audit;

import com.contentgrid.spring.audit.ContentGridAuditEventConfiguration;
import com.contentgrid.spring.boot.autoconfigure.data.web.ContentGridSpringDataRestAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;

@AutoConfiguration(
        after = {
                ContentGridSpringDataRestAutoConfiguration.class,
                RepositoryRestMvcAutoConfiguration.class
        }
)
@ConditionalOnClass({
        ContentGridAuditEventConfiguration.class,
        RepositoryRestMvcConfiguration.class
})
@ConditionalOnBean(RepositoryRestMvcConfiguration.class)
@Import(ContentGridAuditEventConfiguration.class)
public class ContentGridAuditLoggingAutoConfiguration {

}
