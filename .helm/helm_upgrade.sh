#!/bin/sh
helm upgrade github-discussions-crawler ./github-discussions-crawler -f github-discussions-crawler-values.yaml --install --wait
helm upgrade pg-sqlproxy stable/gcloud-sqlproxy -f gcloud-sqlproxy-values.yaml --namespace github-discussions-crawler --install --wait
