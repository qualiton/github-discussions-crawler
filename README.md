# Github Discussions Crawler and Slack Publisher [![CircleCI](https://circleci.com/gh/qualiton/github-discussions-crawler/tree/master.svg?style=shield)](https://circleci.com/gh/qualiton/github-discussions-crawler/tree/master)

Using [Github Team Discussion](https://blog.github.com/2017-11-20-introducing-team-discussions/) is an excellent way of promoting team collaboration. It can be used for [Architecture Decision Records](https://www.thoughtworks.com/radar/techniques/lightweight-architecture-decision-records), team meeting memos, RFCs.

[Slack](https://slack.com/) is the best team collaboration tool out in the market. It excels with huge number of integration capabilities.

Github Discussions Crawler marries both of the worlds by publishing discussion events to a slack channel. It uses [Github API V3](https://developer.github.com/v3/) and [Slack Incoming Webhooks](https://api.slack.com/incoming-webhooks) to the integration.

# Supported events

- New Discussion was discovered
![Alt text](docs/new_discussion.png?raw=true "New Discussion")

- New Comment was discovered
![Alt text](docs/new_comment.png?raw=true "New Comment")

# Prerequisites

- Kubernetes cluster on Google Container Engine (GKE)
- Kubernetes namespace to install the chart
- Postgres SQL database to store Github discussion details
- [Github API token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) with `read:discussion  Read team discussions` permission
- Slack Incoming Webhooks configured for your preferred slack channel.

# Installing the Chart

The chart itself is stored in this github repository in the `gh-pages` branch.
To be able to use it with `helm` you have to add this chart repository to `helm` repository list.

```bash
helm repo add qualiton https://qualiton.github.io/github-discussions-crawler/
```

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

# Running with GCP SQL

To be able to run with GCP SQL you have to install [gcloud-sqlproxy](https://github.com/helm/charts/tree/master/stable/gcloud-sqlproxy) and configure the database settings to point to the proxy.
