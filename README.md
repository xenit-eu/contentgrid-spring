# ContentCloud Spring Boot Starters

[![Apache License 2](https://img.shields.io/github/license/xenit-eu/contentcloud-spring-boot?color=blue)](LICENSE)

ContentCloud's Spring Boot Starters provide a set of Spring Boot Starters to jumpstart your ContentCloud application.

* `eu.xenit.contentcloud.starter:contentcloud-spring-boot-starter` is the primary starter
* `eu.xenit.contentcloud.starter:contentcloud-spring-boot-starter-annotations` contains annotation processing dependencies

## Usage

```gradle
dependencies {
	implementation 'eu.xenit.contentcloud.starter:contentcloud-spring-boot-starter:0.0.1-SNAPSHOT'
	annotationProcessor 'eu.xenit.contentcloud.starter:contentcloud-spring-boot-starter-annotations:0.0.1-SNAPSHOT'
}

```