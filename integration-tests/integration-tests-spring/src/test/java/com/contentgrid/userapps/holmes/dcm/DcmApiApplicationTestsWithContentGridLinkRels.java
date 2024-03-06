package com.contentgrid.userapps.holmes.dcm;

import com.contentgrid.spring.data.rest.hal.CurieProviderCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(properties = {
        "spring.content.storage.type.default=fs"
})
class DcmApiApplicationTestsWithContentGridLinkRels {

    @TestConfiguration
    static class TestConfig {

        @Bean
        CurieProviderCustomizer datamodelCurie() {
            return CurieProviderCustomizer.register("d", "urn:example:datamodel:{suffix}");
        }

    }

    @Test
    void contextLoads() {
    }


}
