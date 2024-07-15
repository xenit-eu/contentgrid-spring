package com.contentgrid.userapps.holmes.dcm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.content.storage.type.default=fs",
		"spring.profiles.active=initContainer"
})
class DcmInitContainerTests {

	@Test
	void contextLoads() {
	}

}
