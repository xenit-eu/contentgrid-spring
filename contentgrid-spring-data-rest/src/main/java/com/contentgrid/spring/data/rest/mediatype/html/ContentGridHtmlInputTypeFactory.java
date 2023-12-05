package com.contentgrid.spring.data.rest.mediatype.html;

import java.time.Instant;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.hateoas.mediatype.InputTypeFactory;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.lang.Nullable;

/**
 * An extended {@link InputTypeFactory} based on {@link HtmlInputType}, with additional support
 * for a set of unmapped types:
 * <ul>
 *     <li>{@link Boolean} maps to {@code checkbox}</li>
 *     <li>{@link Instant} maps to {@code datetime}</li>
 * </ul>
 */
@Slf4j
class ContentGridHtmlInputTypeFactory implements InputTypeFactory {

	@Nullable
	@Override
	public String getInputType(Class<?> type) {

		HtmlInputType inputType = HtmlInputType.from(type);

		if (Boolean.class.equals(type)) {
			inputType = HtmlInputType.CHECKBOX;
		}

		if (Instant.class.equals(type)) {
			return "datetime";
		}

		if (inputType == null) {
			log.trace("Type {} not mapped", type.getSimpleName());
			return null;
		}

		return inputType.value();
	}
}
