package com.contentgrid.spring.data.rest.problem;

import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class ResponseEntityFactory {
    public ResponseEntity<Problem> createResponse(Problem problem) {
        var responseBuilder = ResponseEntity.internalServerError();
        if(problem.getStatus() != null) {
            responseBuilder = ResponseEntity.status(problem.getStatus());
        }

        return responseBuilder.contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

}
