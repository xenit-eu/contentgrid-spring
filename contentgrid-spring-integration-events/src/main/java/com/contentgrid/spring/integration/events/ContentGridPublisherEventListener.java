package com.contentgrid.spring.integration.events;

import javax.persistence.EntityManagerFactory;

import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageType;

public class ContentGridPublisherEventListener
        implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private final ContentGridEventPublisher contentGridEventPublisher;

    public ContentGridPublisherEventListener(ContentGridEventPublisher contentGridEventPublisher, EntityManagerFactory entityManagerFactory) {
        this.contentGridEventPublisher = contentGridEventPublisher;
        
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT)
                .appendListener(this);
        registry.getEventListenerGroup(EventType.POST_UPDATE)
                .appendListener(this);
        registry.getEventListenerGroup(EventType.POST_DELETE)
                .appendListener(this);
    }

//    @PrePersist
//    @PreUpdate
//    @PreRemove
//    private void beforeAnyUpdate(Object entity) {
//    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        contentGridEventPublisher.publish(new ContentGridMessage().application("applicationName")
                .type(ContentGridMessageType.create).entity(event.getEntity()));
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        contentGridEventPublisher.publish(new ContentGridMessage().application("applicationName")
                .type(ContentGridMessageType.update).entity(event.getEntity()));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        contentGridEventPublisher.publish(new ContentGridMessage().application("applicationName")
                .type(ContentGridMessageType.delete).entity(event.getEntity()));
    }

//    @PostLoad
//    private void afterLoad(Object entity) {
//    }
}
