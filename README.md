# Github Discussions Crawler and Slack Publisher [![CircleCI](https://circleci.com/gh/qualiton/github-discussions-crawler/tree/master.svg?style=shield)](https://circleci.com/gh/qualiton/github-discussions-crawler/tree/master)

Using [Github Team Discussion](https://blog.github.com/2017-11-20-introducing-team-discussions/) is an excellent way of promoting team collaboration. It can be used for [Architecture Decision Records](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records), team meeting memos, RFCs.

[Slack](https://slack.com/) is the best team collaboration tool out in the market. It excels with huge number of integration capabilities.

Github Discussions Crawler marries both of the worlds by publishing discussion events to a slack channel. It uses [Github API V3](https://developer.github.com/v3/) and [Slack Incoming Webhooks](https://api.slack.com/incoming-webhooks) for the integration.

## Supported events

- New discussion has been discovered

<a href="url"><img src="docs/new_discussion.png" aligh="left" width="400" ></a>

- New comment has been discovered

<a href="url"><img src="docs/new_comments.png" aligh="left" width="500" ></a>

Both of the events are extracting targeted users and teams by scanning the message body for `@[0-9a-zA-Z]+` or `#[a-z_\\-]+`

## Prerequisites

- Kubernetes cluster with helm/tiller installed
- Kubernetes namespace to install the chart into
- Postgres SQL database to store Github discussion details
- [Github API token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) with `read:discussion  Read team discussions` permission for an account which is member of the discussion we would like to have updates from
- Slack Incoming Webhooks configured for your preferred slack channel.

## Installing the Chart

The chart itself is stored in this github repository in the `gh-pages` branch.
To be able to use it with `helm` you have to add this chart repository to `helm` repository list.

```bash
helm repo add qualiton https://qualiton.github.io/github-discussions-crawler/
```
> **Tip**: verify if helm sees the added chart repository `helm search github-discussions-crawler`

Install from remote URL with the release name `github-discussions-crawler` into namespace `github-discussions-crawler`:

```bash
helm upgrade github-discussions-crawler qualiton/github-discussions-crawler -f values.yaml --install --wait --namespace github-discussions-crawler
```

`values.yaml` should contain every mandatory attributes to be able to populate the k8s secrets.

## values.yaml format

```yaml
github:
  api-token: GITHUB_API_TOKEN
slack:
  api-token: SLACK_API_TOKEN
database:
  jdbc-url: JDBC_URL
  username: USERNAME
  password: PASSWORD
```

Alternatively specify each parameter using the `--set key=value[,key=value]` argument to `helm install`.

## Uninstalling the Chart

To uninstall/delete the `github-discussions-crawler` deployment:

```bash
helm delete github-discussions-crawler
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Running with GCP SQL

To be able to run with GCP SQL you have to install [gcloud-sqlproxy](https://github.com/helm/charts/tree/master/stable/gcloud-sqlproxy) and configure the Crawler database settings to point to the gcloud-sqlproxy.

## Future improvements

- Send personalised slack message to individuals and/or teams
- Convert into a **slack bot** to be able to get team discussion stats directly into a slack room
- Integrate with [Github Reactions Api](https://developer.github.com/v3/reactions/)
- Add more event types like `Discussion has been closed`, `Comment body has changed`

## Documentation

- [Github Team Discussion](https://blog.github.com/2017-11-20-introducing-team-discussions/)
- [Github API token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/)
- [Slack Incoming Webhooks](https://api.slack.com/incoming-webhooks)
- [Cloud SQL Proxy for Postgres](https://cloud.google.com/sql/docs/postgres/sql-proxy)

