# 20. ris-data-migration-image-usage

Date: 2024-08-06

## Status

Proposed

## Context

Currently, the migration of the lookup tables takes place through [migration_schema_local_setup.md](https://github.com/digitalservicebund/ris-backend-service/blob/024019e1e23a753ecf7cc506f516ae067eeb2823/migration_schema_local_setup.md). The problem with the markdown was that whenever changes were documented to the script, the developer had to manually follow those steps, which can be time-consuming for breaking changes.

An improvement to the manual steps was made through the migration script. The migration script should have reduced the need for manual steps and ensured that those steps are up to date when pulling the latest commit and running the script.

This was only a temporary solution to reduce the effort when setting up a new environment, which later helped to reduce feature deployment time and ensures the latest script is used.

However, currently is that these steps still require a lot of unnecessary documentation when facing feature deployment. Observing the `ris-data-migration`, all the commands to import the tables are there, so we can reuse them instead of having duplication. This way, we ensure we have one updated source for this import job.

We would like to challenge the creation of the PostgreSQL image as outlined in [RISDEV-3972](https://digitalservicebund.atlassian.net/browse/RISDEV-3972). Building a custom PostgreSQL image introduces additional maintenance overhead, which does not align with the current requirements of our main project roadmap. Therefore, we propose that this task be reconsidered and ultimately reduced in scope.

## Decision

Instead, we propose running the migration image with the lookup table initialization prior to the backend start. Locally, we can achieve this with Docker Compose, and in the feature deployments, with Kubernetes init containers. Following these steps, we will reuse the already created image and jobs of the ris-data-migration service.

## Consequences

We will be able to clean the migration script, and its documentation will be reduced. We will have the latest image version thanks to our auto-dependency tool management with every run of our local Docker Compose. It will enable easy execution of future ris-data-migration jobs if needed.


