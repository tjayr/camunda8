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

The experiment/cluster folder contains the docker equivalent of the Zeebe cluster Fargate stack described below.

## Deploying on Fargate

Note: CDK v2 is required.

The cdk module contains a CDK application with a number of stacks for deploying Zeebe 8 on AWS Fargate.

```bash
$ cdk ls

zeebe-cluster
zeebe-cluster-with-monitor
zeebe-single


$ cdk diff zeebe-cluster

$ cdk deploy zeebe-cluster

```

`zeebe-single` will create a single Zeebe service (1 task, 1 container) instance on Fargate. The default VPC is used, the container is deployed into a public subnet.
This uses the `camunda/zeebe:8.0.0` from Docker hub. There is no additional configuration applied apart from the environment variables 

`zeebe-cluster`, will deploy 3 Zeebe nodes as Fargate services, a gateway and 2 brokers. The nodes use epheneral task storage instead of an EFS file system volume. 
Service discovery is enabled with a private dns namespace, so nodes can be addressed  using `<brokere-name>.zeebe-cluster`, this is important for the Zeebe cluster to form.
`enableDnsSupport` and `enableDnsHostnames` need to be enabled on the VPC - they usually are for the default VPC, but will need to be set if using a different VPC.

`zeebe-cluster-with-monitor`, same as zeebe-cluster, but also includes the Simple Nonitor application deployed behind an Application Load Balancer

`zeebe-hazelcast-stack`, will deploy Zeebe with the same network infrastructure as zeebe-stack but will also enable Hazelcast exporter.
The Hazelcast exporter is added to custom Docker image and it is saved as a tar in the root project. The docker image will be uploaded to an ECR registry when `cdk deploy` is run
The `build-docker.sh` script will generate the image and tar file. 

```bash
$ ./build-docker.sh

$ cdk deploy zeebe-hazelcast-stack
```

Destroy all resources:

`$ cdk destroy`

**AWS BILL WARNING** - The ECR registry created with the `zeebe-hazlecast-stack` does not get deleted with `cdk destroy`. This needs to be done manually 




