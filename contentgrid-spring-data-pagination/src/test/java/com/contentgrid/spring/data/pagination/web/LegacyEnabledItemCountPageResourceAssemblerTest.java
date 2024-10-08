package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(
        classes = InvoicingApplication.class,
        properties = {
                "contentgrid.security.unauthenticated.allow=true"
        }
)
public class LegacyEnabledItemCountPageResourceAssemblerTest extends AbstractItemCountPageResourceAssemblerTest{

    @Override
    ResultMatcher[] createLegacyResultMatchers(int expectedItemCount) {
        return new ResultMatcher[] {
                MockMvcResultMatchers.jsonPath("$.page.totalElements").value(expectedItemCount),
                MockMvcResultMatchers.jsonPath("$.page.totalPages").value(Math.ceil((double) expectedItemCount /20))
        };
    }
}
