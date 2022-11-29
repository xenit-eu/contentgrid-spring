# ContentGrid customizations for Spring Data REST

[![Apache License 2](https://img.shields.io/github/license/xenit-eu/contentgrid-spring?color=blue)](LICENSE)

This project addresses a number of issues with Spring Data REST controllers in the context of ContentGrid applications.

## Issues with the Association Resource

* Spring Data REST Association Resource responses do not have support for pagination
* Additionally, ContentGrid resolves authorization polices using [Thunx](https://github.com/xenit-eu/thunx). Without
this customization, Thunx only resolves authorization predicates against the root resource, not associated resources.

Both issues are addressed by using an HTTP 302 redirect to the linked repository, instead of replying with the
materialized associated resources inline.

Prerequisites: this redirection (currently) requires that QueryDSL support is enabled on the JPA repositories.

## Getting Started

### Import Configuration

You can import the configuration class `ContentGridSpringDataRestConfiguration.class`, which will use a Spring
`BeanPostProcessor` to replace the `@RepositoryRestController` named `RepositoryPropertyReferenceController`.

```java
@SpringBootApplication
@Import(ContentGridSpringDataRestConfiguration.class)
public class MyApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```