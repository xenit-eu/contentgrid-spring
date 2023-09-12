package com.contentgrid.spring.integration.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest()
@ContextConfiguration(classes = InvoicingApplication.class)
class EntityModelAssemblerTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void serializesEntityWithContentProperties() {
        var assembler = new EntityModelAssembler(context);
        var invoice = new Invoice();

        invoice.setId(UUID.randomUUID());

        var model = assembler.toModel(invoice);

        assertThat(model.getLink(IanaLinkRelations.SELF)).isPresent();
        assertThat(model.getLink("d:content")).isPresent();
        assertThat(model.getLink("d:attachment")).isPresent();
    }

}