package com.contentgrid.spring.data.rest.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Refund;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;

@SpringBootTest(classes = InvoicingApplication.class)
class BeanValidationRepositoryEventListenerTest {

    @Autowired
    BeanValidationRepositoryEventListener eventListener;

    @Nested
    class PropertyValidation {

        @Test
        void createOrSave_valid() {
            var customer = new Customer();
            customer.setVat("BE123");
            assertThatCode(() -> eventListener.onBeforeCreate(customer))
                    .doesNotThrowAnyException();
            assertThatCode(() -> eventListener.onBeforeSave(customer))
                    .doesNotThrowAnyException();
        }

        @Test
        void createOrSave_invalid() {
            var customer = new Customer();
            customer.setName("XYZ");

            assertThatThrownBy(() -> eventListener.onBeforeCreate(customer))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("vat");
                        });
                    });

            assertThatThrownBy(() -> eventListener.onBeforeSave(customer))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("vat");
                        });
                    });
        }

    }

    @Nested
    class RequiredRelation {

        @Test
        void createOrSave_withRequiredRelation() {
            var customer = new Customer();
            customer.setVat("XYZ-1");

            var invoice = new Invoice();
            invoice.setNumber("123");
            invoice.setCounterparty(customer);

            assertThatCode(() -> eventListener.onBeforeCreate(invoice))
                    .doesNotThrowAnyException();
            assertThatCode(() -> eventListener.onBeforeSave(invoice))
                    .doesNotThrowAnyException();
        }

        @Test
        void createOrUnlink_missingRequiredRelation() {
            var invoice = new Invoice();
            invoice.setNumber("123");

            assertThatThrownBy(() -> eventListener.onBeforeCreate(invoice))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("counterparty");
                        });
                    });

            assertThatThrownBy(() -> eventListener.onBeforeLinkDelete(invoice, new Customer()))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("counterparty");
                        });
                    });
        }

        @Test
        void link_requiredRelation() {
            var invoice = new Invoice();
            invoice.setNumber("123");
            invoice.setCounterparty(new Customer());

            assertThatCode(() -> eventListener.onBeforeLinkSave(invoice,
                    invoice.getCounterparty())).doesNotThrowAnyException();
        }

        @Test
        void delete_targetOfRequiredRelation_manyToOne_notLinked() {
            var customer = new Customer();
            customer.setVat("XYZ-8");

            assertThatCode(() -> eventListener.onBeforeDelete(customer)).doesNotThrowAnyException();
        }

        @Test
        void delete_targetOfRequiredRelation_manyToOne_linked() {
            var customer = new Customer();
            customer.setVat("XYZ-8");

            var invoice = new Invoice();
            invoice.setNumber("XYZ-8");
            invoice.setCounterparty(customer);

            customer.getInvoices().add(invoice);

            assertThatThrownBy(() -> eventListener.onBeforeDelete(customer))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("invoices");
                        });
                    });
        }

        @Test
        void delete_targetOfRequiredRelation_oneToOne_unlinked() {
            var customer = new Customer();
            customer.setVat("XYZ-9");

            var invoice = new Invoice();
            invoice.setNumber("XYZ-9");
            invoice.setCounterparty(customer);
            customer.getInvoices().add(invoice);

            assertThatCode(() -> eventListener.onBeforeDelete(invoice)).doesNotThrowAnyException();
        }

        @Test
        void delete_targetOfRequiredRelation_oneToOne_linked() {
            var customer = new Customer();
            customer.setVat("XYZ-9");

            var invoice = new Invoice();
            invoice.setNumber("XYZ-9");
            invoice.setCounterparty(customer);
            customer.getInvoices().add(invoice);

            var refund = new Refund();
            refund.setInvoice(invoice);
            invoice.setRefund(refund);

            assertThatThrownBy(() -> eventListener.onBeforeDelete(invoice))
                    .isInstanceOfSatisfying(RepositoryConstraintViolationException.class, ex -> {
                        assertThat(ex.getErrors().getGlobalErrors()).isEmpty();
                        assertThat(ex.getErrors().getFieldErrors()).satisfiesExactly(fieldError -> {
                            assertThat(fieldError.getField()).isEqualTo("refund");
                        });
                    });
        }
    }
}
