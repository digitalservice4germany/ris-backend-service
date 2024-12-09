# 4. Use Java 17

Date: 2022-05-03

## Status

Superseded by [22. Use Java 21](0022-use-java-21.md)

## Context

The JDK 11 LTS is phasing out in late 2023 already. JDK 17 as the next LTS generation will provide support timeframes until 2026 at least.
JDK 17 provides an accumulated set of recent language, API and JVM enhancements.

## Decision

We're migrating to Java 17.

## Consequences

Due to encapsulation of JDK internal APIs 3rd-party dependencies or own code may no longer have access to particular parts of the JDK,
resulting in errors such as `module jdk.compiler does not export com.sun.tools.javac.processing to unnamed module`.

Thus we may need to upgrade dependencies that use those internals and make sure own code no longer uses them.
As a last resort an `--add-opens` based workaround might become necessary.

With records using Lombok's `@Value` should be avoided.
