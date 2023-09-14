package com.contentgrid.spring.data.rest.validation;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.core.ValidationErrors;
import org.springframework.data.rest.core.event.AbstractRepositoryEventListener;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.OptionalValidatorFactoryBean;

@RequiredArgsConstructor
public class BeanValidationRepositoryEventListener extends AbstractRepositoryEventListener<Object> {

    private final PersistentEntities persistentEntities;
    private final ObjectProvider<Validator> validatorProvider;

    private void validate(Object entity, Class<?>... hints) {
        if (entity == null) {
            return;
        }
        Errors errors = new ValidationErrors(entity, persistentEntities);
        var validator = validatorProvider.getIfAvailable(OptionalValidatorFactoryBean::new);

        if (validator.supports(entity.getClass())) {
            ValidationUtils.invokeValidator(validator, entity, errors, (Object[]) hints);
        }

        if (errors.hasErrors()) {
            throw new RepositoryConstraintViolationException(errors);
        }
    }


    @Override
    protected void onBeforeCreate(Object entity) {
        validate(entity);
    }

    @Override
    protected void onBeforeSave(Object entity) {
        validate(entity);
    }

    @Override
    protected void onBeforeLinkSave(Object parent, Object linked) {
        validate(parent);
    }

    @Override
    protected void onBeforeLinkDelete(Object parent, Object linked) {
        validate(parent);
    }

}
