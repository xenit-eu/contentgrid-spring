package com.contentgrid.spring.data.pagination.web;

import lombok.Value;

@Value
public class CursorPageMetadata {

    String previousCursor;

    String nextCursor;
}
