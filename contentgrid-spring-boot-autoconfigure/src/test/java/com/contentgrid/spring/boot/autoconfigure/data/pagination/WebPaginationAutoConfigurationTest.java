package com.contentgrid.spring.boot.autoconfigure.data.pagination;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.boot.autoconfigure.data.web.ContentGridSpringDataRestAutoConfiguration;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;

class WebPaginationAutoConfigurationTest {

    private static final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ContentGridSpringDataRestAutoConfiguration.class,
                    WebPaginationAutoConfiguration.class,
                    RepositoryRestMvcAutoConfiguration.class
            ));

    @Test
    void pagination_configuration_default_pagination() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            assertThat(context).hasSingleBean(CursorCodec.class);
            assertThat(context.getBean(RepositoryRestConfiguration.class).getPageParamName()).isEqualTo("page");
        });

        contextRunner
                .withPropertyValues("spring.data.rest.page-param-name=my_page")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasSingleBean(CursorCodec.class);
                    assertThat(context.getBean(RepositoryRestConfiguration.class).getPageParamName()).isEqualTo(
                            "my_page");
                });
    }

    @Test
    void pagination_configuration_cursor_pagination() {
        contextRunner
                .withPropertyValues("contentgrid.rest.pagination=page_cursor")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasSingleBean(CursorCodec.class);
                    assertThat(context.getBean(RepositoryRestConfiguration.class).getPageParamName()).isEqualTo(
                            "_cursor");
                });

        contextRunner
                .withPropertyValues("contentgrid.rest.pagination=page_cursor")
                .withPropertyValues("spring.data.rest.page-param-name=my_page")
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    assertThat(context).hasSingleBean(CursorCodec.class);
                    assertThat(context.getBean(RepositoryRestConfiguration.class).getPageParamName()).isEqualTo(
                            "my_page");
                });
    }

    @Test
    void pagination_configuration_custom_codec() {
        contextRunner
                .withBean(CursorCodec.class, () -> Mockito.mock(CursorCodec.class))
                .withPropertyValues("contentgrid.rest.pagination=page_cursor")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(CursorCodec.class);
                    assertThat(context.getBean(RepositoryRestConfiguration.class).getPageParamName()).isEqualTo("page");
                });

    }

}