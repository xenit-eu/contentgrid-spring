package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(
        classes = InvoicingApplication.class,
        properties = {
                "contentgrid.security.unauthenticated.allow=true",
                "contentgrid.rest.expose-legacy-page-info=false"
        }
)
public class LegacyDisabledItemCountPageResourceAssemblerTest extends AbstractItemCountPageResourceAssemblerTest{

    @Override
    ResultMatcher[] createLegacyResultMatchers(int expectedItemCount) {
        return new ResultMatcher[] {
                MockMvcResultMatchers.jsonPath("$.page.totalElements").doesNotExist(),
                MockMvcResultMatchers.jsonPath("$.page.totalPages").doesNotExist()
        };
    }
}
