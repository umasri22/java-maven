# Cloud Run End User Authentication with PostgreSQL Database Sample

This sample integrates with the Identity Platform to authenticate users to the
application and connects to a Cloud SQL postgreSQL database for data storage.

Use it with the [End user Authentication for Cloud Run](http://cloud.google.com/run/docs/tutorials/identity-platform).

For more details on how to work with this sample read the [Google Cloud Run Java Samples README](https://github.com/GoogleCloudPlatform/java-docs-samples/tree/main/run).

[![Run on Google Cloud](https://deploy.cloud.run/button.svg)](https://deploy.cloud.run/)

## Dependencies

* **Spring Boot**: Web server framework.
* **Spring Cloud GCP**: PostgreSQL, Logging
* **Logback + SLF4J**: Logging framework  
* **Thymeleaf** Java template engine
* **Jib**: Container build tool
* **firebase-admin**: Verifying JWT token
* **okhttp + google-auth-library**: Access [compute metadata server](https://cloud.google.com/compute/docs/storing-retrieving-metadata) for project Id
* **Lombok**: Generate getters and setters
* **Firebase JavaScript SDK**: client-side library for authentication flow

## Environment Variables

Cloud Run services can be [configured with Environment Variables](https://cloud.google.com/run/docs/configuring/environment-variables).
Required variables for this sample include:

* `CLOUD_SQL_CREDENTIALS_SECRET`: the resource ID of the secret, in format: `projects/PROJECT_ID/secrets/SECRET_ID/versions/VERSION` when deployed to Cloud Run. At runtime, Cloud Run will inject the secret value as an environment variable, for more info see [Using secrets](https://cloud.google.com/run/docs/configuring/secrets#command-line). See [postgres-secrets.json](postgres-secrets.json) for secret content.

OR

Uncomment variables in `application.properties` and set:
* `CLOUD_SQL_CONNECTION_NAME`: Cloud SQL instance name, in format: `<MY-PROJECT>:<INSTANCE-REGION>:<MY-DATABASE>`
* `DB_NAME`: Cloud SQL postgreSQL database name
* `DB_USER`: database user
* `DB_PASSWORD`: database password

## Production Considerations

* Both `postgres-secrets.json` and `static/config.js` should not be committed to
  a git repository and should be added to `.gitignore`.

* Saving credentials directly as environment variables is convenient for local testing,
  but not secure for production; therefore using `SECRET_NAME` and `VERSION`
  in combination with the Google Secret Manager is recommended.  

## Running Locally

```
mvn spring-boot:run
```
