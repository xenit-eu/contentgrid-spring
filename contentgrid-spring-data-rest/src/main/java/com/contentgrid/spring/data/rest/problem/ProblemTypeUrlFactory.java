package com.contentgrid.spring.data.rest.problem;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.UriTemplate;

@RequiredArgsConstructor
class ProblemTypeUrlFactory {

    private final UriTemplate baseUri;

    public URI resolve(ProblemTypeResolvable type) {
        return baseUri.expand(List.of(type.getProblemHierarchy()));
    }

    public interface ProblemTypeResolvable {

        String[] getProblemHierarchy();
    }

}
