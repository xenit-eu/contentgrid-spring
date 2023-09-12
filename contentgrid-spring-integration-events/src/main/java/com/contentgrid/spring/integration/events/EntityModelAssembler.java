package com.contentgrid.spring.integration.events;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
import org.springframework.data.util.Lazy;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.RepresentationModelProcessorInvoker;

public class EntityModelAssembler {

    private final SpelAwareProxyProjectionFactory projectionFactory;

    private final Lazy<RepresentationModelProcessorInvoker> representationModelProcessorInvoker;
    private final Lazy<PersistentEntities> persistentEntities;
    private final Lazy<Associations> associations;
    private final Lazy<SelfLinkProvider> selfLinkProvider;
    private final Lazy<RepositoryRestConfiguration> repositoryRestConfiguration;

    private PersistentEntityResourceAssembler assembler;

    public EntityModelAssembler(ApplicationContext context) {
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        projectionFactory.setBeanFactory(context);
        projectionFactory.setBeanClassLoader(this.getClass().getClassLoader());

        this.representationModelProcessorInvoker = Lazy.of(
                () -> context.getBean(RepresentationModelProcessorInvoker.class));
        this.persistentEntities = Lazy.of(() -> context.getBean(PersistentEntities.class));
        this.associations = Lazy.of(() -> context.getBean(Associations.class));
        this.selfLinkProvider = Lazy.of(() -> context.getBean(SelfLinkProvider.class));
        this.repositoryRestConfiguration = Lazy
                .of(() -> context.getBean(RepositoryRestConfiguration.class));
    }

    private PersistentEntityResourceAssembler getAssembler() {
        if (assembler != null) {
            return assembler;
        }

        PersistentEntityProjector projector = new PersistentEntityProjector(
                repositoryRestConfiguration.get().getProjectionConfiguration(), projectionFactory,
                null, associations.get().getMappings());

        this.assembler = new PersistentEntityResourceAssembler(persistentEntities.get(), projector,
                associations.get(), selfLinkProvider.get());

        return assembler;
    }

    public EntityModel<Object> toModel(Object entity) {
        var representationModel = getAssembler().toModel(entity);
        return representationModelProcessorInvoker.get().invokeProcessorsFor(representationModel);
    }

}
