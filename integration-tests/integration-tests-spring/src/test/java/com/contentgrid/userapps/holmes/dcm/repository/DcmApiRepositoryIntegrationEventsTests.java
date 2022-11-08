package com.contentgrid.userapps.holmes.dcm.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import javax.persistence.EntityManagerFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher;
import com.contentgrid.spring.integration.events.ContentGridPublisherEventListener;
import com.contentgrid.userapps.holmes.dcm.model.Case;

@SpringBootTest(properties = { "spring.content.storage.type.default=fs" })
class DcmApiRepositoryIntegrationEventsTests {

    @TestConfiguration
    public static class TestConfig {

        @Bean
        ContentGridPublisherEventListener contentGridPublisherEventListenerSpyMock(
                EntityManagerFactory entityManagerFactory, ContentGridEventPublisher publisher) {
            ContentGridPublisherEventListener spy2 = spy(
                    new ContentGridPublisherEventListener(publisher, entityManagerFactory));
            return spy2;
        }

    }

    @Autowired
    CaseRepository repository;

    @Autowired
    ContentGridPublisherEventListener listener;

    @BeforeEach
    public void buildSpy() {
        // since we are using spring injection we have to reset our spy mock before each
        // test
        reset(listener);
    }

    @Test
    void whenCaseIsSavedOnce_postInsertShouldBeCalledOnce_ok() {
        repository.save(new Case());
        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
    }

    @Test
    void whenCaseIsSavedTwice_postInsertShouldBeCalledTwice_ok() {
        repository.save(new Case());
        repository.save(new Case());
        verify(listener, times(2)).onPostInsert(any());
        verify(listener, times(0)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
    }

    @Test
    void whenCaseIsUpdatedOnce_postUpdateShouldBeCalledOnce_ok() {
        Case saved = repository.save(new Case());
        saved.setDescription("description for update");
        repository.save(saved);

        verify(listener, times(1)).onPostInsert(any());
        verify(listener, times(1)).onPostUpdate(any());
        verify(listener, times(0)).onPostDelete(any());
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
    }
}
