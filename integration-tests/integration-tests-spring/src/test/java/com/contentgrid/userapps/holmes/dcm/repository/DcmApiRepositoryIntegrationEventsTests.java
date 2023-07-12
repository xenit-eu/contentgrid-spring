package com.contentgrid.userapps.holmes.dcm.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.repository.support.Repositories;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.test.mock.MockIntegration;
import org.springframework.messaging.MessageHandler;

import com.contentgrid.spring.integration.events.ContentGridEventHandlerProperties;
import com.contentgrid.spring.integration.events.ContentGridEventHandlerProperties.EventProperties;
import com.contentgrid.spring.integration.events.ContentGridEventHandlerProperties.SystemProperties;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher;
import com.contentgrid.spring.integration.events.ContentGridMessageHandler;
import com.contentgrid.spring.integration.events.ContentGridPublisherEventListener;
import com.contentgrid.userapps.holmes.dcm.model.Case;
import com.contentgrid.userapps.holmes.dcm.model.Person;
import com.contentgrid.userapps.holmes.dcm.model.Evidence;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.MessageHeaders;

@SpringBootTest(properties = { "spring.content.storage.type.default=fs" })
class DcmApiRepositoryIntegrationEventsTests {

    @TestConfiguration
    public static class TestConfig {

        @Bean
        ContentGridPublisherEventListener contentGridPublisherEventListenerSpyMock(
                EntityManagerFactory entityManagerFactory, ContentGridEventPublisher publisher, Repositories repositories) {

            ContentGridEventHandlerProperties properties = new ContentGridEventHandlerProperties();
            SystemProperties systemProperties = new ContentGridEventHandlerProperties.SystemProperties();
            systemProperties.setApplicationId("test");
            systemProperties.setDeploymentId("test");
            
            EventProperties eventProperties = new ContentGridEventHandlerProperties.EventProperties();
            eventProperties.setWebhookConfigUrl("http://test/actuator/webhooks");
            
            properties.setSystem(systemProperties);
            properties.setEvents(eventProperties);

            ContentGridPublisherEventListener spy2 = spy(
                    new ContentGridPublisherEventListener(publisher, entityManagerFactory, repositories));
            return spy2;
        }

        @Bean
        ContentGridMessageHandler messageHandler(ObjectMapper objectMapper) {

            MessageHandler messageHandler = MockIntegration.mockMessageHandler().handleNext(m -> {
                Object payload = m.getPayload();
                assertThat(payload).isInstanceOf(String.class);

                // check headers
                MessageHeaders headers = m.getHeaders();
                assertThat(headers).containsKey("application_id");
                assertThat(headers).containsKey("deployment_id");
                assertThat(headers).containsKey("trigger");
                assertThat(headers).containsKey("entity");
                assertThat(headers).containsKey("webhookConfigUrl");

                try {
                    // check message body
                    HashMap<String, Object> readValue = objectMapper.readValue((String) payload,
                            new TypeReference<HashMap<String, Object>>() {
                            });

                    assertThat(readValue).containsKey("old");
                    assertThat(readValue).containsKey("new");
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });

            MessageHandlerSpec mockMessageHandlerSpec = mock(MessageHandlerSpec.class);
            when(mockMessageHandlerSpec.get()).thenReturn(messageHandler);

            return () -> mockMessageHandlerSpec;
        }
    }

    @Autowired
    CaseRepository repository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    EvidenceRepository evidenceRepository;

    @Autowired
    ContentGridPublisherEventListener listener;

    @Autowired
    ContentGridMessageHandler contentGridMessageHandler;

    @BeforeEach
    public void buildSpy() {
        // since we are using spring injection we have to reset our spy mock before each
        // test
        reset(listener, contentGridMessageHandler.get().get());
    }

    @Test
    void whenCaseIsSavedOnce_postInsertShouldBeCalledOnce_ok() {
        repository.save(new Case());
        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(1)).handleMessage(any());
    }

    @Test
    void whenCaseIsSavedTwice_postInsertShouldBeCalledTwice_ok() {
        repository.save(new Case());
        repository.save(new Case());
        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(2)).handleMessage(any());
    }

    @Test
    void whenCaseIsUpdatedOnce_postUpdateShouldBeCalledOnce_ok() {

        Case case1 = new Case();
        case1.setDescription("old description");

        Case saved = repository.save(case1);
        saved.setDescription("description for update");
        repository.save(saved);

        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(1)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(2)).handleMessage(any());
    }

    @Test
    void whenCaseIsUpdatedTwice_postUpdateShouldBeCalledTwice_ok() {
        Case saved = repository.save(new Case());
        saved.setDescription("description for update");
        repository.save(saved);

        saved.setDescription("description for second update");
        repository.save(saved);

        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(2)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }

    @Test
    void whenCaseIsDeleted_postUpdateShouldBeCalledOnce_ok() {
        Case saved = repository.save(new Case());

        saved.setDescription("description for update");
        repository.save(saved);

        repository.delete(saved);

        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(1)).onPostUpdate(any());
        verify(listener, times(1)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }

    @Test
    void whenPersonIsAddedToCaseSuspects_manyToMany_postUpdateCollectionShouldBeCalledOnce_ok() {
        Case _case = repository.save(new Case());
        Person person = personRepository.save(new Person());

        _case.setSuspects(List.of(person));
        repository.save(_case);
        personRepository.save(person);

        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(1)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }

    @Test
    void whenPersonIsAddedToCaseDetective_manyToOne_postUpdateShouldBeCalledOnce_ok() {
        Case _case = repository.save(new Case());
        Person person = personRepository.save(new Person());

        _case.setLeadDetective(person);
        repository.save(_case);
        personRepository.save(person);

        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(1)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }

    @Test
    void whenEvidenceIsAddedToCase_oneToMany_postUpdateCollectionShouldBeCalledOnce_ok() {
        Case _case = repository.save(new Case());
        Evidence evidence = evidenceRepository.save(new Evidence());

        _case.setHasEvidence(List.of(evidence));
        repository.save(_case);
        evidenceRepository.save(evidence);

        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(1)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }

    @Test
    void whenEvidenceIsAddedToCaseScenario_oneToOne_postUpdateCollectionShouldBeCalledOnce_ok() {
        Case _case = repository.save(new Case());
        Evidence evidence = evidenceRepository.save(new Evidence());

        _case.setScenario(evidence);
        repository.save(_case);
        evidenceRepository.save(evidence);

        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(1)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
        verify(listener, times(0)).onPostUpdateCollection(any());

        verify(contentGridMessageHandler.get().get(), times(3)).handleMessage(any());
    }
}
