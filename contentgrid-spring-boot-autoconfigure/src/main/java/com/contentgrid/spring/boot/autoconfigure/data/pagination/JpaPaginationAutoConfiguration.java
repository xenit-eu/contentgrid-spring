package com.contentgrid.spring.boot.autoconfigure.data.pagination;

import com.contentgrid.spring.data.pagination.jpa.ContentGridSpringDataPaginationJpaConfiguration;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.JpaRepositoryConfigExtension;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

// These annotations are taken from JpaRepositoriesAutoConfiguration
@AutoConfiguration(before = JpaRepositoriesAutoConfiguration.class, after = {HibernateJpaAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class})
@ConditionalOnClass({JpaRepository.class, ContentGridSpringDataPaginationJpaConfiguration.class})
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean({JpaRepositoryFactoryBean.class, JpaRepositoryConfigExtension.class})
@ConditionalOnProperty(prefix = "spring.data.jpa.repositories", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import({
        ContentGridSpringDataPaginationJpaConfiguration.class,
        JpaPaginationAutoConfiguration.JpaRepositoriesImportSelector.class
})
public class JpaPaginationAutoConfiguration {

    // define this bean just so the built-in JpaRepositoriesAutoConfiguration does not trigger.
    // otherwise all repositories will be registered *twice*, which will cause conflicts
    @Bean
    JpaRepositoryConfigExtension jpaRepositoryConfigExtension() {
        return new JpaRepositoryConfigExtension();
    }

    static class JpaRepositoriesImportSelector implements ImportSelector {

        @Override
        public String[] selectImports(AnnotationMetadata importingClassMetadata) {
            // EnableJpaRepositories must be done via a registrar, otherwise the package that contains the annotated configuration is scanned for repositories
            // (which is incorrect; we want the package of the spring boot application to be scanned, not our library)
            return new String[]{JpaRepositoriesRegistrar.class.getName()};
        }

    }

}
