# 6. Backend and SPA in monorepo

Date: 2022-05-12

## Status

Accepted

## Context

We are building an application that consist of a single-page application (SPA) as frontend along with a Java based Backend For Frontend (BFF). The question where to keep the sources for both stacks needed to be addressed.

## Decision

We are starting with keeping all of the sources in a single repository, i.e. monorepo. The frontend/SPA sources reside in a `frontend` directory. This is so that we avoid the additional complexity that would come with an approach where sources are kept in separate repositories:

- An SPA in a separate repository will need its own deployment (Nginx, S3 bucket etc.) and pipeline.
- The SPA is no longer secured out-of-the-box by Spring's security config; we'd need to recreate it or already deploy a shared auth implementation from the very beginning.
- It wasn't immediately clear when to run E2E tests as post deployment verification and where these tests should be maintained.
- Thus, it seemed feasible to maintain E2E tests in a separate repository as well and run it as post deployment verification whenever backend **or** frontend sources have been pushed from their respective repositories. Maintenance is further spread out. Alternatively, the SPA is going to provide a consumer driven contract, which the backend will run as part of their pipeline, so that there is early feedback when the contract between the SPA and BFF was broken (accidently or not). Both approaches increase the risk of introducing additional friction and coordination effort.

## Consequences

As described, deployment becomes easier, generally speaking. The CI/CD pipeline will take longer to run for the monorepo approach.
