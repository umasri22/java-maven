# Eventarc Samples

[![Open in Cloud Shell][shell_img]][shell_link]

[shell_img]: http://gstatic.com/cloudssh/images/open-btn.png
[shell_link]: https://console.cloud.google.com/cloudshell/open?git_repo=https://github.com/GoogleCloudPlatform/java-docs-samples&page=editor&open_in_editor=blog/README.md

This directory contains samples for creating Cloud Run services that listens to events from [Eventarc](https://cloud.google.com/eventarc/docs/).

## Samples

|           Sample                |        Description       |     Deploy    |
| ------------------------------- | ------------------------ | ------------- |
|[Eventarc - Pub/Sub](pubsub/) | Listen to Pub/Sub events | [<img src="https://storage.googleapis.com/cloudrun/button.svg" alt="Run on Google Cloud" height="30">][run_button_events_pubsub] |
|[Eventarc - Audit – Storage](audit-storage/) | Listen to Audit Log events from Cloud Storage | [<img src="https://storage.googleapis.com/cloudrun/button.svg" alt="Run on Google Cloud" height="30">][run_button_events_audit_storage] |
|[Eventarc - Generic](generic/) | Listen to a generic event from Eventarc | [<img src="https://storage.googleapis.com/cloudrun/button.svg" alt="Run on Google Cloud" height="30">][run_button_events_generic] |

## Setup

1. [Set up for Cloud Run development](https://cloud.google.com/run/docs/setup)

1. Clone this repository:

    ```
    git clone https://github.com/GoogleCloudPlatform/java-docs-samples.git
    ```

1. In the samples's `pom.xml`, update the image field for the `jib-maven-plugin`
with your Google Cloud Project Id:

    ```XML
    <plugin>
      <groupId>com.google.cloud.tools</groupId>
      <artifactId>jib-maven-plugin</artifactId>
      <version>2.0.0</version>
      <configuration>
        <to>
          <image>gcr.io/PROJECT_ID/SAMPLE_NAME</image>
        </to>
      </configuration>
    </plugin>
    ```

## How to run a sample locally

1. [Build the sample container using Jib](https://github.com/GoogleContainerTools/jib):

    ```Bash
    mvn compile jib:dockerBuild
    ```

1. [Run containers locally](https://cloud.google.com/run/docs/testing/local) by
replacing `PROJECT_ID` and `SAMPLE_NAME` with your values.

    With the built container:

    ```Bash
    PORT=8080 && docker run --rm -p 9090:${PORT} -e PORT=${PORT} gcr.io/PROJECT_ID/SAMPLE_NAME
    ```

    Injecting your service account key for access to GCP services:

    ```Bash
    PORT=8080 && docker run \
       -p 9090:${PORT} \
       -e PORT=${PORT} \
       -e K_SERVICE=dev \
       -e K_CONFIGURATION=dev \
       -e K_REVISION=dev-00001 \
       -e GOOGLE_APPLICATION_CREDENTIALS=/tmp/keys/[FILE_NAME].json \
       -v $GOOGLE_APPLICATION_CREDENTIALS:/tmp/keys/[FILE_NAME].json:ro \
        gcr.io/PROJECT_ID/SAMPLE_NAME
    ```

    * Use the --volume (-v) flag to inject the credential file into the container
      (assumes you have already set your `GOOGLE_APPLICATION_CREDENTIALS`
      environment variable on your machine)

    * Use the --environment (-e) flag to set the `GOOGLE_APPLICATION_CREDENTIALS`
      variable inside the container

1. Open http://localhost:9090 in your browser.

Learn more about [testing your container image locally.][testing]

## Deploying

1. [Build the sample container using Jib](https://github.com/GoogleContainerTools/jib):

    ```
    mvn compile jib:build
    ```

    **Note**: Using the image tag `gcr.io/PROJECT_ID/SAMPLE_NAME` automatically
    pushes the image to [Google Container Registry](https://cloud.google.com/container-registry/).

1. Deploy to Cloud Run by replacing `PROJECT_ID` and `SAMPLE_NAME` with your values:

    ```bash
    gcloud run deploy --image gcr.io/PROJECT_ID/SAMPLE_NAME
    ```

[run_button_events_audit_storage]: https://deploy.cloud.run/?git_repo=https://github.com/GoogleCloudPlatform/java-docs-samples&dir=eventarc/audit-storage
[run_button_events_pubsub]: https://deploy.cloud.run/?git_repo=https://github.com/GoogleCloudPlatform/java-docs-samples&dir=eventarc/pubsub
[run_button_events_generic]: https://deploy.cloud.run/?git_repo=https://github.com/GoogleCloudPlatform/java-docs-samples&dir=eventarc/generic
[testing]: https://cloud.google.com/run/docs/testing/local#running_locally_using_docker_with_access_to_services
