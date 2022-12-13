package com.contentgrid.spring.data.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.test.fixture.invoicing.model.QInvoice;
import org.junit.jupiter.api.Test;
import org.springframework.data.querydsl.binding.QuerydslBindings;

class QuerydslBindingsInspectorTest {

    @Test
    void defaultBinding() {
        var inspector = new QuerydslBindingsInspector(new QuerydslBindings());
        assertThat(inspector.findPathBindingFor(QInvoice.invoice.counterparty)).hasValue("counterparty");
    }

    @Test
    void excludingBinding() {
        var bindings = new QuerydslBindings();
        bindings.excluding(QInvoice.invoice.counterparty);

        var inspector = new QuerydslBindingsInspector(bindings);
        assertThat(inspector.findPathBindingFor(QInvoice.invoice.counterparty)).isEmpty();
    }


    @Test
    void aliasedBinding() {
        var bindings = new QuerydslBindings();
        bindings.bind(QInvoice.invoice.counterparty).as("customer").withDefaultBinding();

        var inspector = new QuerydslBindingsInspector(bindings);
        assertThat(inspector.findPathBindingFor(QInvoice.invoice.counterparty)).isEmpty();

        // we cannot resolve aliased bindings right now
        // it would be awesome if the outcome of this test could be flipped
        // assertThat(inspector.findPathBindingFor(QInvoice.invoice.counterparty)).isNotEmpty();
    }

}