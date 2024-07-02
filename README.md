# Spring modules for ContentGrid Applications

[![Apache License 2](https://img.shields.io/github/license/xenit-eu/contentgrid-spring?color=blue)](LICENSE)

This project contains Spring modules that are used by ContentGrid applications.

## Getting Started

This project provides a Spring Boot starters, which is the most convenient way to jumpstart your ContentGrid
application.

```gradle
dependencies {
	implementation 'com.contentgrid.spring:contentgrid-spring-boot-starter:0.2.0-SNAPSHOT'
	annotationProcessor 'com.contentgrid.spring:contentgrid-spring-boot-starter-annotations:0.2.0-SNAPSHOT'
}
```

## Modules

* `contentgrid-automations-rest` serve the automation configurations of ContentGrid applications
* `contentgrid-spring-boot-actuators` serve the authorization policy and webhook configuration as actuator endpoints
* `contentgrid-spring-boot-autoconfigure` contains Spring Boot AutoConfiguration for ContentGrid applications
* `contentgrid-spring-boot-platform` is a Bill of Materials for ContentGrid applications,
  extending `spring-boot-dependencies`
* `contentgrid-spring-boot-starter` is the primary Spring Boot Starter
* `contentgrid-spring-boot-starter-annotations` is a (required) starter for annotation processing
* `contentgrid-spring-data-rest` customizes Spring Data REST to address a number of issues
* `contentgrid-spring-integration-events` publishes database change events on a message queue
* `contentgrid-spring-audit-logging` publishes audit events for REST API access on a message queue

