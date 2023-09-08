package com.contentgrid.spring.data.rest.validation;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.Validator;

@RequiredArgsConstructor
public class BeanValidationRepositoryRestConfigurer implements RepositoryRestConfigurer {
    @NonNull
    private final Validator validator;

    @Override
    public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener validatingListener) {
        validatingListener.addValidator("beforeCreate", validator);
        validatingListener.addValidator("beforeSave", validator);
    }
}
