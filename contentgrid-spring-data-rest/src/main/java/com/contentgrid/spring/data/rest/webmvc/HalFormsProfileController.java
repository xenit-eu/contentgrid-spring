package com.contentgrid.spring.data.rest.webmvc;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.TemplateVariable.VariableType;
import org.springframework.hateoas.TemplateVariables;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.Affordances;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RequiredArgsConstructor
@BasePathAwareController
public class HalFormsProfileController implements InitializingBean {

    private final RepositoryRestConfiguration configuration;
    private final EntityLinks entityLinks;
    private final DomainTypeToHalFormsPayloadMetadataConverter toHalFormsPayloadMetadataConverter;
    private final ObjectMapper objectMapper;

    private static final Class<?> HAL_FORMS_TEMPLATE_CLASS;

    static {
        try {
            HAL_FORMS_TEMPLATE_CLASS = Class.forName("org.springframework.hateoas.mediatype.hal.forms.HalFormsTemplate");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = RequestMethod.OPTIONS, produces = MediaTypes.HAL_FORMS_JSON_VALUE)
    HttpEntity<?> halFormsOptions() {
        var headers = new HttpHeaders();

        headers.setAllow(Collections.singleton(HttpMethod.GET));

        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    @RequestMapping(value = ProfileController.RESOURCE_PROFILE_MAPPING, method = RequestMethod.GET, produces = {
            MediaTypes.HAL_FORMS_JSON_VALUE
    })
    void halFormsProfile(RootResourceInformation information, HttpServletResponse response)
            throws IOException {
        var model = new RepresentationModel<>();

        model.add(Link.of(ProfileController.getPath(configuration, information.getResourceMetadata())));

        var collectionLink = entityLinks.linkFor(information.getDomainType())
                .withRel(IanaLinkRelations.DESCRIBES)
                .withName(IanaLinkRelations.COLLECTION_VALUE);

        var placeholder = "---" + UUID.randomUUID() + "---";
        var itemLinkTemplate = entityLinks.linkToItemResource(information.getDomainType(), placeholder)
                .getTemplate()
                .with("id", VariableType.SIMPLE);

        var itemLink = Link.of(
                UriTemplate.of(itemLinkTemplate.toString().replace(placeholder, ""), new TemplateVariables(itemLinkTemplate.getVariables())),
                IanaLinkRelations.DESCRIBES
        ).withName(IanaLinkRelations.ITEM_VALUE);

        var collectionAffordances = Affordances.of(collectionLink)
                .afford(HttpMethod.HEAD) // This is to pin down the default affordance, which we don't care about
                .andAfford(HttpMethod.POST)
                .withName(IanaLinkRelations.CREATE_FORM_VALUE)
                .withInput(toHalFormsPayloadMetadataConverter.convertToCreatePayloadMetadata(information.getDomainType()))
                .withInputMediaType(MediaType.MULTIPART_FORM_DATA)
                .andAfford(HttpMethod.PATCH) // This gets mapped to "GET" with the very ugly hack below
                .withName(IanaLinkRelations.SEARCH_VALUE)
                .withInput(toHalFormsPayloadMetadataConverter.convertToSearchPayloadMetadata(information.getDomainType()))
                .build();

        model.add(collectionAffordances.toLink());
        model.add(itemLink);

        response.setContentType(MediaTypes.HAL_FORMS_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), model);
    }

    @Override
    public void afterPropertiesSet() {
        objectMapper.registerModule(new SimpleModule().setSerializerModifier(new CustomHalFormsTemplateSerializerModifier()));
    }

    @JsonSerialize(using = CustomHalFormsTemplateSerializer.class)
    private static final class HalFormsTemplateMixin {

    }

    private static class CustomHalFormsTemplateSerializerModifier extends BeanSerializerModifier {

        @Override
        @SuppressWarnings("unchecked")
        public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                JsonSerializer<?> serializer) {
            if(Objects.equals(beanDesc.getBeanClass(), HAL_FORMS_TEMPLATE_CLASS)) {
                return new CustomHalFormsTemplateSerializer((JsonSerializer<Object>) serializer);
            }
            return serializer;
        }
    }

    /**
     * This is what is unfortunately required to change the hal-forms method to GET, without having to re-implement too many things ourself.
     *
     * Because:
     * - hal-forms templates that would be created from GET are filtered out
     * - hal-forms templates that have a method other than POST/PUT/PATCH have their template properties cleared out
     * - We can't change the serialization of HttpMethod itself, as the enum itself is not directly rendered, only its string value is.
     * - We can't subclass/override HalFormsTemplate directly, since it's package-private
     */
    @RequiredArgsConstructor
    private static class CustomHalFormsTemplateSerializer extends JsonSerializer<Object> {

        private final static Field HTTP_METHOD_FIELD;

        static {
            try {
                HTTP_METHOD_FIELD = HAL_FORMS_TEMPLATE_CLASS.getDeclaredField("httpMethod");
                HTTP_METHOD_FIELD.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        private final JsonSerializer<Object> defaultSerializer;

        @SneakyThrows
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if(HTTP_METHOD_FIELD.get(value) == HttpMethod.PATCH) {
                HTTP_METHOD_FIELD.set(value, HttpMethod.GET);
            }
            defaultSerializer.serialize(value, gen, serializers);
        }
    }
}
