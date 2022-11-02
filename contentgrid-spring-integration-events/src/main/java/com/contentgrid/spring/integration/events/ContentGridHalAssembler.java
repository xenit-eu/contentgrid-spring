package com.contentgrid.spring.integration.events;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.mapping.Associations;
import org.springframework.data.rest.webmvc.support.PersistentEntityProjector;
import org.springframework.data.util.Lazy;

public class ContentGridHalAssembler {

    private final SpelAwareProxyProjectionFactory projectionFactory;

    private PersistentEntityResourceAssembler assembler;
    private Lazy<PersistentEntities> persistentEntities;
    private Lazy<Associations> associations;
    private Lazy<SelfLinkProvider> selfLinkProvider;
    private Lazy<RepositoryRestConfiguration> repositoryRestConfiguration;

    public ContentGridHalAssembler(ApplicationContext context) {
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
        projectionFactory.setBeanFactory(context);
        projectionFactory.setBeanClassLoader(this.getClass().getClassLoader());

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

    public PersistentEntityResource toModel(Object entity) {
        return getAssembler().toModel(entity);
    }
}
