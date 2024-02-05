package com.contentgrid.spring.integration.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.contentgrid.spring.integration.events.TestConfig.TestMessageHandler;
import com.contentgrid.spring.test.fixture.invoicing.InvoicingApplication;
import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import com.contentgrid.spring.test.fixture.invoicing.model.Order;
import com.contentgrid.spring.test.fixture.invoicing.model.PromotionCampaign;
import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import com.contentgrid.spring.test.fixture.invoicing.repository.CustomerRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.InvoiceRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.OrderRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.PromotionCampaignRepository;
import com.contentgrid.spring.test.fixture.invoicing.repository.ShippingAddressRepository;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = {InvoicingApplication.class, TestConfig.class})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class EntityChangeHibernateEventListenerTest {
    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    PromotionCampaignRepository promotionCampaignRepository;

    @Autowired
    InvoiceRepository invoiceRepository;

    @Autowired
    ShippingAddressRepository shippingAddressRepository;

    @Autowired
    TestMessageHandler testMessageHandler;

    @Autowired
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void resetMessageHandler() {
        testMessageHandler.reset();
    }

    private static Customer customer(Consumer<Customer> configure) {
        var customer = new Customer();
        configure.accept(customer);
        return customer;
    }

    private static Invoice invoice(Consumer<Invoice> configure) {
        var invoice = new Invoice();
        configure.accept(invoice);
        return invoice;
    }

    private static PromotionCampaign promotionCampaign(Consumer<PromotionCampaign> configure) {
        var promotionCampaign = new PromotionCampaign();
        configure.accept(promotionCampaign);
        return promotionCampaign;
    }

    private static ThrowingConsumer<Message<?>> checkEvent(String trigger, Class<?> domainType) {
        return message -> {
            assertThat(message.getHeaders()).extractingByKey("trigger").isEqualTo(trigger);
            assertThat(message.getHeaders()).extractingByKey("entity").isEqualTo(domainType.getName());
        };
    }

    @Test
    void whenCustomerIsSavedOnce_postInsertShouldBeCalledOnce_ok() {
        customerRepository.save(customer(c -> c.setVat("BE123")));

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class)
        );
    }

    @Test
    void whenCustomerIsSavedTwice_postInsertShouldBeCalledTwice_ok() {
        customerRepository.save(customer(c -> c.setVat("BE456")));
        customerRepository.save(customer(c -> c.setVat("BE789")));

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class),
                checkEvent("create", Customer.class)
        );
    }

    @Test
    void whenCustomerIsUpdatedOnce_postUpdateShouldBeCalledOnce_ok() {

        Customer customer1 = new Customer();
        customer1.setVat("BE124");
        customer1.setName("old name");

        Customer saved = customerRepository.save(customer1);
        saved.setName("updated name");
        customerRepository.save(saved);

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class),
                checkEvent("update", Customer.class)
        );
    }

    @Test
    void whenCustomerIsUpdatedTwice_postUpdateShouldBeCalledTwice_ok() {
        Customer saved = customerRepository.save(customer(c -> c.setVat("BE125")));
        saved.setName("description for update");
        saved = customerRepository.save(saved);

        saved.setName("description for second update");
        customerRepository.save(saved);

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class),
                checkEvent("update", Customer.class),
                checkEvent("update", Customer.class)
        );
    }

    @Test
    void whenCustomerIsDeleted_postUpdateShouldBeCalledOnce_ok() {
        Customer saved = customerRepository.save(customer(c -> c.setVat("BE126")));

        saved.setName("description for update");
        saved = customerRepository.save(saved);

        customerRepository.delete(saved);

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class),
                checkEvent("update", Customer.class),
                checkEvent("delete", Customer.class)
        );
    }

    @Test
    void whenPromoIsAddedToOrderPromos_manyToMany_postUpdateCollectionShouldBeCalledOnce_ok() {
        UUID orderId = orderRepository.save(new Order()).getId();
        // We have to run this in a transaction and verify the interactions _after_ the transaction closes, because
        // events are sent after committing the tx. If we don't run it in a transaction, hibernate complains about
        // a lack of an open session when we try to do anything with order.promos.
        transactionTemplate.executeWithoutResult((status) -> {
            // Order has a Set<PromotionCampaign> that is initialized to null. To avoid a PostUpdate event being sent because
            // hibernate hydrated that to an empty PersistentBag, we save and get Order first before calling anything on it.
            Order order = orderRepository.findById(orderId).orElseThrow();
            PromotionCampaign promotionCampaign = promotionCampaignRepository.save(promotionCampaign(c -> c.setPromoCode("ABC-123")));

            order.addPromo(promotionCampaign);
            orderRepository.save(order);
            promotionCampaignRepository.save(promotionCampaign);
        });

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Order.class),
                checkEvent("create", PromotionCampaign.class),
                checkEvent("update", Order.class)
        );
    }

    @Test
    void whenCustomerIsAddedToOrderCustomer_manyToOne_postUpdateShouldBeCalledOnce_ok() {
        Order order = orderRepository.save(new Order());
        Customer customer = customerRepository.save(customer(c -> c.setVat("BE127")));

        order.setCustomer(customer);
        orderRepository.save(order);

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Order.class),
                checkEvent("create", Customer.class),
                checkEvent("update", Order.class)
        );
    }

    @Test
    void whenOrderIsAddedToInvoice_oneToMany_postUpdateShouldBeCalledOnce_ok() {
        // We have to run this in a transaction and verify the interactions _after_ the transaction closes, because
        // events are sent after committing the tx. If we don't run it in a transaction, hibernate complains about
        // a lack of an open session when we try to do anything with case.hasEvidence.
        transactionTemplate.executeWithoutResult((status) -> {
            var customer = customerRepository.save(customer(c -> c.setVat("BE128")));
            UUID invoiceId = invoiceRepository.save(invoice(i -> {
                i.setCounterparty(customer);
                i.setNumber("X888");
            })).getId();
            Invoice invoice = invoiceRepository.getReferenceById(invoiceId);
            Order order = orderRepository.save(new Order(customer));

            Set<Order> l = new HashSet<>();
            l.add(order);
            invoice.setOrders(l);
            invoiceRepository.save(invoice);
        });


        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Customer.class),
                checkEvent("create", Invoice.class),
                checkEvent("create", Order.class),
                checkEvent("update", Invoice.class)
        );
    }

    @Test
    void whenShippingAddressIsAddedToOrderScenario_oneToOne_postUpdateShouldBeCalledOnce_ok() {
        Order order = orderRepository.save(new Order());
        ShippingAddress shippingAddress = shippingAddressRepository.save(new ShippingAddress());

        order.setShippingAddress(shippingAddress);
        orderRepository.save(order);

        assertThat(testMessageHandler.messages()).satisfiesExactly(
                checkEvent("create", Order.class),
                checkEvent("create", ShippingAddress.class),
                checkEvent("update", Order.class)
        );

    }

}