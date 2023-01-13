package com.contentgrid.spring.test.fixture.invoicing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.data.rest.webmvc.ContentGridSpringDataRestConfiguration;

@SpringBootApplication
public class InvoicingApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoicingApplication.class, args);
    }

}
