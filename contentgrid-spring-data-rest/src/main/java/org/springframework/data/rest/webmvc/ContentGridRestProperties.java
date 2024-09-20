package org.springframework.data.rest.webmvc;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class ContentGridRestProperties {
    boolean fallbackToDefaultRelationController = false;
    boolean useMultipartHalForms = false;
    boolean exposeLegacyPageInfo = true;
    PaginationType pagination = PaginationType.PAGE_NUMBER;

    @Getter
    @RequiredArgsConstructor
    public enum PaginationType {
        PAGE_NUMBER(false),
        PAGE_CURSOR(true);

        private final boolean cursorBased;

    }
}
