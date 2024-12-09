# Migration through Docker

> **Work in progress - import through Docker image**

The following job is importing the minimally required data (refdata and juris tables)

To be able to pull the `ris-data-migration` image, log in to the GitHub Package Repository using your username and a
credential token stored in 1Password (1PW):

If you don't have a personal access token, read here on how to
generate [one](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry#authenticating-with-a-personal-access-token-classic).

```shell
export CR_PAT=$(op read op://Employee/CR_PAT/password)
echo $CR_PAT | docker login ghcr.io -u USERNAME --password-stdin # Replace USERNAME with your GitHub username
```

The following step requires an OTC access token, read here for
more [info](https://platform-docs.prod.ds4g.net/user-docs/how-to-guides/access-obs-via-aws-sdk/#step-2-obtain-access_key-credentials).

To connect to your S3 bucket, ensure your AWS credentials are stored in 1Password, and then set the following
environment variables in your shell:

```shell
export AWS_ACCESS_KEY_ID=$(op read op://Employee/AWS_ACCESS_KEY_ID/password) # Fetch AWS Access Key ID from 1PW
export AWS_SECRET_ACCESS_KEY=$(op read op://Employee/AWS_SECRET_ACCESS_KEY/password) # Fetch AWS Secret Access Key from 1PW
export AWS_BUCKET_NAME=$(op read op://Employee/AWS_BUCKET_NAME/password) # Fetch AWS Bucket Name from 1PW
```

## Run with Docker:

> **Info:**
> Currently not running automatically on ./run.sh dev script

Run manually with:

```bash
docker compose up initialization
```
