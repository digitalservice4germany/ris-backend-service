# 3. Use Spring WebFlux reactive stack

Date: 2021-10-06

## Status

Superseded by [16. Identify Reactive WebFlux as Technical Debt in Caselaw](0016-Identify-Reactive-WebFlux-as-Technical-Dept-in-Caselaw.md)

## Context

We aim for a backend stack that can scale with fewer hardware resources and handle concurrency with
a small number of threads. A reactive application also fits well into microservice architectures
where services communicate asynchronously via messages.

## Decision

We will use [Spring WebFlux](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html) as a web framework.

## Consequences

WebFlux provides powerful abstractions for concurrency, resulting in concise, more composable code;
the declarative programming style further aids readability.

Coming from an object-oriented, imperative programming background there's a steep learning curve in
the shift to non-blocking, functional, and declarative programming; a shift in mindset from
object-oriented thinking (e.g. data encapsulation, message passing, state) to functional thinking
(pipeline-based transformations, immutable data) is required.

Non-blocking drivers or libraries might not be available, for instance AWS DynamoDB or
[Togglz](https://github.com/togglz/togglz/issues/360).
