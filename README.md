# Camunda 8 Examples

This project contains multiple Zeebe spring boot workers that implement the service tasks required in the hello-world.bpm process.

`service1` -  deploys the bpmn diagram from `service1/src/main/resources/hello-world.bpmn`, implements the `Hello` service task

`service2` -  implements the `World` service task

## Requirements

- Docker compose 1.29
- Download zeeba-hazelcast-exporter fat jar to the root of this project:

`wget -O zeebe-hazelcast-exporter-1.1.0-jar-with-dependencies.jar https://github.com/camunda-community-hub/zeebe-hazelcast-exporter/releases/download/1.1.0/zeebe-hazelcast-exporter-1.1.0-jar-with-dependencies.jar`

The provider docker-compose.yml will spin up Zeebe, Elasticsearch and the [Simple Monitor](https://github.com/camunda-community-hub/zeebe-simple-monitor). 

Zeebe is configured with the [Hazelcast exporter](https://github.com/camunda-community-hub/zeebe-hazelcast-exporter) in memory exporter, settings are in the `application.yaml`

## Running 

`docker-compose --profile in-memory up`

Simple Monitor is running at [http://localhost:8085](http://localhost:8085)

Once the Camunda stack is up and running. Launch the 2 Spring boot applications `com.github.thar.camunda.Service1Application` and `com.github.thar.camunda.Service2Application` 

Trigger the hello world process either from Simple Monitor or the Camunda Modeler 

The process requires an input variable called `source` , the json value can be anything e.g: `source = {"name": "John"}`

## Experimental

The experimental folder contains docker-compose files for exporting data from Zeebe to Opensearch instead of Elasticsearch.  

`client-app` is a module for interacting with the Zeebe api to deploy and start processes.