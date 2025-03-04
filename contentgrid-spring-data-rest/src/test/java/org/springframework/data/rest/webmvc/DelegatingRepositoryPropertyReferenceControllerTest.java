package org.springframework.data.rest.webmvc;

import com.contentgrid.spring.boot.autoconfigure.integration.EventsAutoConfiguration;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.security.WithMockJwt;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest(properties = {
        "spring.content.storage.type.default=fs",
        "server.servlet.encoding.enabled=false" // disables mock-mvc enforcing charset in request
}, classes = {
        InvoicingApplication.class
})
@EnableAutoConfiguration(exclude = EventsAutoConfiguration.class)
@AutoConfigureMockMvc
@WithMockJwt
class DelegatingRepositoryPropertyReferenceControllerTest {

    @TestConfiguration
    @EntityScan(basePackageClasses = DelegatingRepositoryPropertyReferenceControllerTest.class)
    class TestConfig {

    }

    @Autowired
    MockMvc mockMvc;


    @SneakyThrows
    private String createObject(String url) {
        return mockMvc.perform(MockMvcRequestBuilders.post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn()
                .getResponse()
                .getRedirectedUrl();
    }

    @Test
    void manyToManyRelation_followed_sameRelationName() throws Exception {
        var sourceEntity1 = createObject("/source-entity1s");
        var sourceEntity2 = createObject("/source-entity2s");
        var targetEntity = createObject("/target-entities");

        mockMvc.perform(MockMvcRequestBuilders.post(sourceEntity1+"/items")
                .contentType("text/uri-list")
                .content(targetEntity)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        mockMvc.perform(MockMvcRequestBuilders.post(sourceEntity2+"/items")
                .contentType("text/uri-list")
                .content(targetEntity)
        ).andExpect(MockMvcResultMatchers.status().is2xxSuccessful());

        mockMvc.perform(MockMvcRequestBuilders.get(sourceEntity1+"/items"))
                .andExpect(MockMvcResultMatchers.redirectedUrlPattern("http://localhost/target-entities?_internal_sourceEntity1__items=*"));

        mockMvc.perform(MockMvcRequestBuilders.get(sourceEntity2+"/items"))
                .andExpect(MockMvcResultMatchers.redirectedUrlPattern("http://localhost/target-entities?_internal_sourceEntity2__items=*"));
    }

}