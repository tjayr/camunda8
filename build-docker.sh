#!/bin/bash

docker build -t zeebe-hazelcast ./docker

docker save zeebe-hazelcast:latest | gzip > zeebe-hazelcast.tar.gz