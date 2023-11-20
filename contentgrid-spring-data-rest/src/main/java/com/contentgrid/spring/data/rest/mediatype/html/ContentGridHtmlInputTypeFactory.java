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
 *     <li></li>
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
			inputType = HtmlInputType.DATETIME_LOCAL;
		}

		if (inputType == null) {
			log.warn("Type {} not mapped", type.getSimpleName());
			return null;
		}

		return inputType.value();
	}
}