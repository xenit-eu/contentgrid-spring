package com.contentgrid.spring.integration.events;

import com.contentgrid.spring.integration.events.EntityChangeEventPublisher.EntityChangeEvent;
import com.contentgrid.spring.integration.events.EntityChangeEventPublisher.EntityChangeEvent.ChangeKind;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostCollectionUpdateEvent;
import org.hibernate.event.spi.PostCollectionUpdateEventListener;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.repository.support.Repositories;

public class EntityChangeHibernateEventListener implements PostInsertEventListener,
        PostUpdateEventListener, PostDeleteEventListener, PostCollectionUpdateEventListener,
        InitializingBean {

    private final EntityChangeEventPublisher entityChangeEventPublisher;
    private final EntityManagerFactory entityManagerFactory;
    private final Repositories repositories;

    public EntityChangeHibernateEventListener(EntityChangeEventPublisher entityChangeEventPublisher,
            EntityManagerFactory entityManagerFactory, Repositories repositories) {
        this.entityChangeEventPublisher = entityChangeEventPublisher;
        this.entityManagerFactory = entityManagerFactory;
        this.repositories = repositories;
    }

    @Override
    public void afterPropertiesSet() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_COLLECTION_UPDATE).appendListener(this);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        entityChangeEventPublisher.publish(
                EntityChangeEvent.builder()
                        .trigger(ChangeKind.create)
                        .domainType(deriveDomainType(event.getEntity()))
                        .newEntity(event.getEntity())
                        .build()
        );
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        Object oldEntity = BeanUtils.instantiateClass(entity.getClass());
        BeanUtils.copyProperties(entity, oldEntity);
        event.getPersister().setPropertyValues(oldEntity, event.getOldState());

        entityChangeEventPublisher.publish(
                EntityChangeEvent.builder()
                        .trigger(ChangeKind.update)
                        .domainType(deriveDomainType(entity))
                        .oldEntity(oldEntity)
                        .newEntity(entity)
                        .build()
        );
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        entityChangeEventPublisher.publish(
                EntityChangeEvent.builder()
                        .trigger(ChangeKind.delete)
                        .domainType(deriveDomainType(event.getEntity()))
                        .oldEntity(event.getEntity())
                        .build()
        );
    }

    @Override
    public void onPostUpdateCollection(PostCollectionUpdateEvent event) {
        entityChangeEventPublisher.publish(
                EntityChangeEvent.builder()
                        .trigger(ChangeKind.update)
                        .domainType(deriveDomainType(event.getAffectedOwnerOrNull()))
                        .oldEntity(event.getAffectedOwnerOrNull())
                        .newEntity(event.getAffectedOwnerOrNull())
                        .build()
        );
    }

    private Class<?> deriveDomainType(Object entity) {
        return repositories.getPersistentEntity(entity.getClass()).getType();
    }

}
