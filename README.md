# Boost Greetings Service [![Chat on slack](https://img.shields.io/badge/chat-on_slack-40ad85.svg?style=flat&logoWidth=14&logo=slack)](https://ovoenergy.slack.com/messages/boost-balances) [![CircleCI](https://circleci.com/gh/ovotech/boost-service-template/tree/master.svg?style=shield&circle-token=be07de09424d5683a9afdfaaf2cad706333d2228)](https://circleci.com/gh/ovotech/boost-service-template/tree/master)

The purpose of this project to generate and publish greetings based on incoming people.

## Links
- Prod GCP Console :exclamation:
- Prod Grafana :exclamation:
- Nonprod GCP Console :exclamation:
- Nonprod Grafana :exclamation:

## Build locally
- Clone the project
- Set up a [bintray](https://bintray.com) account
- Ask Read and Consumptions team to assign your account with ovotech account
- Approve the linking Request
- Create a .bintray-credentials file into your home folder, with the following content:
```
realm=Bintray
host=dl.bintray.com
user={ACCOUNT_ID}
password={API_TOKEN}
``` 
Substitute ACCOUNT_ID and API_TOKEN with your. Your API_TOKEN can be found at your bintray profile.
- Test the project in the same shell you configured docker
```bash
sbt all-tests
```

## Run locally
Before you start playing with greetings service please set up your local Google Cloud Platform as described [here](https://cloud.google.com/sdk/docs/quickstart-macos)

### Obtain credentials
There are two ways of getting the credentials:
- Ask an team member for the service account credentials.
- Create and download a key for the default service account in the [Google Cloud IAM](https://console.cloud.google.com/iam-admin/serviceaccounts/project?project=boost-nonprod) section in JSON format.

### Setup and Run
#### IntelliJ
Create a run configuration for the module you want to run, adding the following **Environment Variable**  

| Name  | Value|
| ------------- | ------------- |
| GOOGLE_APPLICATION_CREDENTIALS  | {FULL_PATH_TO_YOUR_KEY}  |

Open an sbt shell and run **refreshToken** to refresh the GCP token

#### Terminal
```bash
export GOOGLE_APPLICATION_CREDENTIALS={PATH_TO_YOUR_KEY}
sbt runServer
```

## Release
Release process is managed by the customized [sbt-release](https://github.com/sbt/sbt-release) plugin and [Helm](https://docs.helm.sh/using_helm/#quickstart-guide)
```bash
sbt "release with-defaults"
```
Following steps are taken during the release process
- Checks if the working directory doesn't have uncommited changes
- Changes version number to the **release version** in [version.sbt](version.sbt) and [Chart.yaml](.helm/chart/Chart.yaml). It is need for the correct docker images and helm versions for the release
- Calls **docker:publish** to publish new release docker image to remote repository
- Commit changes and tag repository with release version number
- Changes version number to the **next SNAPSHOT version** in [version.sbt](version.sbt) and [Chart.yaml](.helm/chart/Chart.yaml). It is needed for being able to test features with SNAPSHOT version without releasing
- Commit and push changes to remote git repository

## Circle CI Requirements
The project should have a Google service account (boost-circleci) with the following permissions:

- Kubernetes Engine Admin
- Storage Admin

## Circle CI settings
This job requires the following environment variable:
- BINTRAY_USER - btt-ci
- BINTRAY_PASSWORD - btt-ci access token for bintray
- PROD_GCLOUD_ACCOUNT_AUTH - base64 encoded PROD project boost-circleci service account credentials
- NONPROD_GCLOUD_ACCOUNT_AUTH - base64 encoded NONPROD project boost-circleci service account credentials
- NONPROD_APP_SECRETS - base64 encoded application secrets in NONPROD from 1password/boost-greetings-service NONPROD_APP_SECRETS
- PROD_APP_SECRETS - base64 encoded application secrets in PROD 1password/boost-greetings-service PROD_APP_SECRETS
- NONPROD_INFLUX_PASSWORD - INFLUX_PASSWORD from NONPROD_APP_SECRETS `without encoding`
- PROD_INFLUX_PASSWORD - INFLUX_PASSWORD from PROD_APP_SECRETS `without encoding`
- gcp_registry_auth_json_key - NONPROD project boost-circleci service account credentials `without encoding` (this is the project where we keep boost images)
- shipit_api_key - api key for `https://shipit.ovotech.org.uk` to inform shipit about deployments

## Architecture Overview
This diagram shows application dependencies
![Alt text](docs/404-not-found-error.png?raw=true "Overview")

## Modules
- [server](server) is a module where we spin up the server for our project. It contains mandatory endpoins
- [greeting](greeting) is a module where core greetings-related infrastructure and business logic is stored
- [database](database) is a module where liquibase database description and related code is stored
- [messages](messages) contains formats for incoming Kafka messages
- [.helm](.helm) module contains [Helm](https://docs.helm.sh/using_helm/#using-helm) chart definition for release
- [.circleci](.circleci) module contains Circle Ci pipeline definition

## Main Principles
- Our main driving approach is [Domain Driven Design](https://en.wikipedia.org/wiki/Domain-driven_design)
- We extensively use [Hexagonal Architecture](https://www.infoq.com/news/2013/04/DDD-Architecture-Styles) to be able to *implement a well-defined and ring-fenced domain layer*
- We embrace [Property-based testing](http://blog.jessitron.com/2013/04/property-based-testing-what-is-it.html) to have wider test case coverage
- [Boy Scout Rule](http://programmer.97things.oreilly.com/wiki/index.php/The_Boy_Scout_Rule) - Continuous and iterative improvement of our codebase

## Technology stack
- [Scala 2.12](http://www.scala-lang.org/) is the *backbone* of our applications
- [Typelevel](http://typelevel.org/projects/) to be able to write *more functional* and *less boilerplate* code
- [ScalaTest](http://www.scalatest.org/) and [ScalaCheck](https://www.scalacheck.org/) for *unit and property based testing*
- [Gatling](http://gatling.io/#/) to be able to run *performance tests*
- [Helm](https://docs.helm.sh/using_helm/#quickstart-guide) The Kubernetes Package Manager

## Known issues
- From time to time you have to run **kubectl get pods** since the default google **GCPAuthenticator** doesn't implement the functionality to refresh the token!
The issue is addressed by an sbt taskkey (runServer) in [Aliases.scala](project/Aliases.scala) but it's better to know about it!
- When you terminate the JVM meanwhile there is an ongoing test with docker, there is a high chance for some containers to stay in running mode after the JVM terminated. It might be in issue for the next test run. Use **sbt ddi** to clean up your docker environment before running tests again!

## Contributing Policy
We welcome any contribution as long as it complies with our [contributing policy](CONTRIBUTING.md)

