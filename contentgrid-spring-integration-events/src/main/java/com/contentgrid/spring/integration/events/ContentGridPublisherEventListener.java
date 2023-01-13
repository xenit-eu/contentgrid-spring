package com.contentgrid.spring.integration.events;

import java.util.Map;

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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;

import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.ContentGridMessageType;
import com.contentgrid.spring.integration.events.ContentGridEventPublisher.ContentGridMessage.DataEntity;

public class ContentGridPublisherEventListener implements PostInsertEventListener,
        PostUpdateEventListener, PostDeleteEventListener, InitializingBean {

    private final ContentGridEventPublisher contentGridEventPublisher;
    private final EntityManagerFactory entityManagerFactory;
    private final ContentGridEventHandlerProperties applicationProperties;

    public ContentGridPublisherEventListener(ContentGridEventPublisher contentGridEventPublisher,
            EntityManagerFactory entityManagerFactory, ContentGridEventHandlerProperties applicationProperties) {
        this.contentGridEventPublisher = contentGridEventPublisher;
        this.entityManagerFactory = entityManagerFactory;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void afterPropertiesSet() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_UPDATE).appendListener(this);
        registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(this);
    }
    
    @Override
    @SuppressWarnings("deprecation")
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return this.requiresPostCommitHandling(persister);
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        contentGridEventPublisher
                .publish(new ContentGridMessage(applicationProperties.getSystem().getApplicationId(), applicationProperties.getSystem().getDeploymentId(), 
                        ContentGridMessageType.create, new DataEntity(null, event.getEntity()), event.getEntity().getClass(), 
                        Map.of("webhookConfigUrl", "http://host.docker.internal:6060/actuator/webhooks")));
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        Object oldEntity = BeanUtils.instantiateClass(entity.getClass());
        BeanUtils.copyProperties(entity, oldEntity);
        event.getPersister().setPropertyValues(oldEntity, event.getOldState());

        contentGridEventPublisher
                .publish(new ContentGridMessage(applicationProperties.getSystem().getApplicationId(), applicationProperties.getSystem().getDeploymentId(), 
                        ContentGridMessageType.update, new DataEntity(oldEntity, entity), event.getEntity().getClass(), 
                        Map.of("webhookConfigUrl", "http://host.docker.internal:6060/actuator/webhooks")));
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        contentGridEventPublisher
                .publish(new ContentGridMessage(applicationProperties.getSystem().getApplicationId(), applicationProperties.getSystem().getDeploymentId(), 
                        ContentGridMessageType.delete, new DataEntity(event.getEntity(), null), event.getEntity().getClass(), 
                        Map.of("webhookConfigUrl", "http://host.docker.internal:6060/actuator/webhooks")));
    }
}
