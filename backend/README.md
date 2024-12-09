# Backend

Java service built with Spring Boot.

## Development

### Run Service

Requires the all but backend to be running in docker:

```bash
# optionally add -d or --detached for detached mode
../run.sh dev --no-backend
```

**Start backend with IntelliJ:**

- Use the checked in run config in `../.idea/runConfigurations/`. If you open just this `backend`
  folder with IntelliJ, you will need to copy and adjust it.
- Alternatively:
  - Install the [EnvFile plugin](https://plugins.jetbrains.com/plugin/7861-envfile)
  - Add a Run/Debug configuration for Spring Boot
  - Set the active profile to `local`
  - Activate EnvFile and also activate the "Experimental integration" checkbox.
  - Add your `.env` file from project root to the list

**Start backend with VS Code:**

- The launch config in `.vscode/launch.json` should be used automatically

**Start backend from CLI:**

```bash
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

> **Note**
>
> The application depends on a Java package from a private GitHub package repository. To be able to
> download it in the Gradle build process, you'll need to set up your local env as described in
> the [root readme](../README.md#setup-local-environment).

### Database Setup & Migration with Flyway

The application uses Flyway for maintaining and versioning database migrations.

Most of the caselaw database structure is setup through ris-data-migration repo -
see [instructions here](../migration_schema_local_setup.md). This repo manages only norms tables and
those with data that does not exist in migrated documentation units (e.g. publication reports, .docx
files).

In order to create a change in the database you should follow one of the two methods:

1. If you want to create a migration using SQL:
   - You should create a new sql file on the directory `src\main\resources\db\migration`.
   - The file should be named in the following format: `Vx.x__teamname_create_table_xyz.sql`
     where `x.x` is your migration version (make sure to pull first from the repository and see
     what is the latest version otherwise migrations wouldn't work properly).
2. If you want to create a migration using Java:
   - You should create a new Java class on the directory `src\main\java\db\migration` that
     extends `BaseJavaMigration` and implements the
     method `void migrate(Context context) throws Exception`.
   - The file should be named in the following format: `Vx_x__teamname_create_table_xyz.sql`
     where `x_x` is your migration version (make sure to pull first from the repository and see
     what is the latest version in the sql path and the java path otherwise migrations wouldn't
     work properly).

For both migration file names, the `teamname` can be replaced with: whether `caselaw` or `norms` and
is normally followed by a descriptive name for the migration.

Flyway automatically detects new files and run migrations accordingly on sprint boot start.

## Tests

The project has distinct unit and integration test sets.

**To run just the unit tests:**

```bash
./gradlew test
```

**To run the integration tests:**

```bash
./gradlew integrationTest
```

> **Note**
>
> Running integration tests requires passing unit tests (in Gradle terms: integration tests depend
> on unit tests), so unit tests are going to be run first. In case there are failing unit tests we
> won't attempt to continue running any integration tests.

**To run integration tests exclusively, without the unit test dependency:**

```bash
./gradlew integrationTest --exclude-task test
```

Denoting an integration test is accomplished by using a JUnit 5 tag
annotation: `@Tag("integration")`.

Furthermore, there is another type of test worth mentioning. We're
using [ArchUnit](https://www.archunit.org/getting-started) for ensuring certain architectural
characteristics, for instance making sure that there are no cyclic dependencies.

## Formatting & Styleguide

Check
our [Java Styleguides](https://digitalservicebund.atlassian.net/wiki/spaces/VER/pages/1088913456/Backend+Conventions)
document. To set up IntelliJ IDEA
follow [these instructions](https://github.com/google/google-java-format#intellij-android-studio-and-other-jetbrains-ides).
Consistent formatting, for Java as well as various other types of source code, is being enforced
via [Spotless](https://github.com/diffplug/spotless).

**Check formatting:**

```bash
./gradlew spotlessCheck
```

**Autoformat sources:**

```bash
./gradlew spotlessApply
```

## Code quality analysis

Continuous code quality analysis is performed in the pipeline upon pushing to trunk; it requires a
token provided as `SONAR_TOKEN` repository secret that needs to be obtained
from https://sonarcloud.io.

**To run the analysis locally:**

```bash
SONAR_TOKEN=[sonar-token] ./gradlew sonar
```

Go
to [https://sonarcloud.io](https://sonarcloud.io/dashboard?id=digitalservicebund_ris-backend-service)
for the analysis results.

## Container image

Container images running the application are automatically published by the pipeline to
the [GitHub Packages Container registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry).

**To run the latest published image:**

```bash
docker run -p8080:8080 "ghcr.io/digitalservicebund/ris-backend-service:$(git log -1 origin/main --format='%H')"
```

The service will be accessible at `http://localhost:8080`.

We are using Spring's built-in support for producing an optimized container image:

```bash
./gradlew bootBuildImage
docker run -p8080:8080 ghcr.io/digitalservicebund/ris-backend-service
```

Container images in the registry
are [signed with keyless signatures](https://github.com/sigstore/cosign/blob/main/KEYLESS.md).

**To verify an image:**

```bash
COSIGN_EXPERIMENTAL=1 cosign verify "ghcr.io/digitalservicebund/ris-backend-service:$(git log -1 origin/main --format='%H')"
```

If you need to push a new container image to the registry manually there are two ways to do this:

**Via built-in Gradle task:**

```bash
export CONTAINER_REGISTRY=ghcr.io
export CONTAINER_IMAGE_NAME=digitalservicebund/ris-backend-service
export CONTAINER_IMAGE_VERSION="$(git log -1 --format='%H')"
CONTAINER_REGISTRY_USER=[github-user] CONTAINER_REGISTRY_PASSWORD=[github-token] ./gradlew bootBuildImage --publishImage
```

**Note:** Make sure you're using a GitHub token with the necessary `write:packages` scope for this
to work.

**Using Docker:**

```bash
echo [github-token] | docker login ghcr.io -u [github-user] --password-stdin
docker push "ghcr.io/digitalservicebund/ris-backend-service:$(git log -1 --format='%H')"
```

**Note:** Make sure you're using a GitHub token with the necessary `write:packages` scope for this
to work.

## Vulnerability Scanning

Scanning container images for vulnerabilities is performed
with [Trivy](https://github.com/aquasecurity/trivy)
as part of the pipeline's `build` job, as well as each night for the latest published image in the
container
repository.

To run a scan locally:

```bash
./gradlew bootBuildImage
TRIVY_DB_REPOSITORY="ghcr.io/aquasecurity/trivy-db,public.ecr.aws/aquasecurity/trivy-db" \
TRIVY_JAVA_DB_REPOSITORY="ghcr.io/aquasecurity/trivy-java-db,public.ecr.aws/aquasecurity/trivy-java-db" \
trivy image --severity HIGH,CRITICAL ghcr.io/digitalservicebund/ris-backend-service:latest
```

Run with `--format json` to get an extended report.

## License Scanning

License scanning is performed as part of the pipeline's `build` job. Whenever a production
dependency
is being added with a yet unknown license the build is going to fail.

**To run a scan locally:**

```bash
./gradlew checkLicense
```

## Dependency Updates

Can be used to show the latest release version for every dependency. Generate a report
in `build/dependencyUpdates/report.txt`.

> [!IMPORTANT]
> Only update final release. Exclude alpha beta or RC (Release Candidate) if possible.

**To run a scan locally:**

```bash
./gradlew dependencyUpdates -Drevision=release
```
