#!/bin/sh
mkdir -p ~/.ssh
sudo ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts && \
git config --global user.name "lachatak" && \
git config --global user.email "krisztian.lachata@gmail.com"
