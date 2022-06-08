# Deploying Zeebe / Camunda 8 on AWS Fargate

## Diagrams

## Zeebe Configuration

```java

```

### Modeling Zeebe for Fargate Deployment

Zeebe clusters do not form dynamically, the broker nodes need to be configured to be aware of each other by specifying the list of host names.
A Zeebe gateway needs to have knowledege of the hostname for 1 of the broker nodes and from this node it can determine the rest of the cluster topology.

When deploying on Fargate this creates a problem as task ip addresses are dynamically assigned. To get around this you can use Cloudmap for service discovery.

I created a private DNS name space (with the same name as the ECS cluster) into which each service is registered. Now I have a hostname pattern to use in configuration:    

`<service-name>.<private-namespace>:<port>`

### Single Tasks vs Multiple Tasks and Services?

The question often arises of what containers to put inside an ECS task. I chose to put each Zeebe broker/gateway 
inside its own task definition and deploy them individually as services.

The Fargate service takes care of automatically restarting the task in the event of a crash. A service is also required 
if you want to expose a container through a load balancer - the Zeebe gateway for example.

Could you put the entire Zeebe cluster inside a single task definition? Yes absolutely, the tradeoff of this 
approach is the task is should one of the containers inside the task fail, the entire task will be restarted, which will result in down time.

### Create a task definition o for a Gateway node

### Create a task definition for a Broker

## Service Discovery

Zeebe cluster formation

Hostnames

Load balancing

Certificates
