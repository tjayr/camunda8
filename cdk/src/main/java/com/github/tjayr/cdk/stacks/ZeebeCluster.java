package com.github.tjayr.cdk.stacks;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;
import software.amazon.awscdk.services.servicediscovery.PrivateDnsNamespace;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZeebeCluster extends Stack {

    private static final String ECS_CLUSTER_NAME="zeebe-cluster";
    private PrivateDnsNamespace dnsNamespace;
    private SecurityGroup clusterSecurityGroup;

    public ZeebeCluster(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var vpc = Vpc.fromLookup(this, "default-vpc", VpcLookupOptions.builder().isDefault(true).build());

        clusterSecurityGroup = SecurityGroup.Builder.create(this, "sg-zeebe-cluster")
                .allowAllOutbound(true)
                .securityGroupName("zeebe-cluster-dev-sg")
                .vpc(vpc)
                .build();
        clusterSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcpRange(26500, 26502));
        clusterSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.udpRange(26500, 26502));
        clusterSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(5701));
        clusterSecurityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(9600));
        clusterSecurityGroup.applyRemovalPolicy(RemovalPolicy.DESTROY);

        this.dnsNamespace = PrivateDnsNamespace.Builder.create(this, "private-dns").name(ECS_CLUSTER_NAME)
                .vpc(vpc).description("Zeebe Cluster DNS namespace")
                .build();

        var zeebeCluster = Cluster.Builder.create(this, "zeebe-cluster")
                .clusterName(ECS_CLUSTER_NAME)
                .vpc(vpc)
                .build();

        //createGateway(zeebeCluster);
        createBroker(zeebeCluster, 0);
        createBroker(zeebeCluster, 1);
        createBroker(zeebeCluster, 2);

    }

    private FargateService createGateway(ICluster cluster) {

        return FargateService.Builder.create(this, "zeebe-gateway")
                .cluster(cluster)
                .desiredCount(1)
                .minHealthyPercent(100).maxHealthyPercent(200)
                .deploymentController(DeploymentController.builder().type(DeploymentControllerType.ECS).build())
                .assignPublicIp(true)
                .serviceName("zeebe-gateway")
                .taskDefinition(gatewayTaskDefinition())
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("zeebe-gateway")
                        .cloudMapNamespace(dnsNamespace)
                        .dnsRecordType(DnsRecordType.A).build())
                .securityGroups(List.of(clusterSecurityGroup))
                .build();
    }

    private FargateService createBroker(ICluster cluster, int node) {
        return FargateService.Builder.create(this, "zeebe-broker-"+node)
                .cluster(cluster)
                .desiredCount(1)
                .minHealthyPercent(100).maxHealthyPercent(200)
                .deploymentController(DeploymentController.builder().type(DeploymentControllerType.ECS).build())
                .assignPublicIp(true)
                .serviceName("zeebe-broker-"+node)
                .taskDefinition(brokerTaskDefinition(node))
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .cloudMapOptions(CloudMapOptions.builder()
                        .name("zeebe-broker-"+node)
                        .cloudMapNamespace(dnsNamespace)
                        .dnsRecordType(DnsRecordType.A).build())
                .securityGroups(List.of(clusterSecurityGroup))
                .build();
    }


    private FargateTaskDefinition brokerTaskDefinition( int zeebeNodeId) {

        var dataVolume = Volume.builder().name("zeebe-broker-data-"+zeebeNodeId).build();

        var td = FargateTaskDefinition.Builder.create(this, "zeebe-broker-task-def-"+zeebeNodeId)
                .cpu(512)
                .memoryLimitMiB(1024)
                .family("dev-zeebe")
                .volumes(List.of(dataVolume))
                .build();
        td.applyRemovalPolicy(RemovalPolicy.DESTROY);

        var brokerConf = new HashMap<String, String>();
        brokerConf.put("JAVA_TOOL_OPTIONS", "-Xms512m -Xmx512m " );
        brokerConf.put("ZEEBE_BROKER_CLUSTER_NODEID", String.valueOf(zeebeNodeId));
        brokerConf.put("ZEEBE_BROKER_DATA_DISKUSAGECOMMANDWATERMARK", "0.998");
        brokerConf.put("ZEEBE_BROKER_DATA_DISKUSAGEREPLICATIONWATERMARK", "0.999");
        brokerConf.put("ZEEBE_BROKER_NETWORK_HOST", "0.0.0.0");
        brokerConf.put("ZEEBE_BROKER_CLUSTER_PARTITIONSCOUNT", "2");
        brokerConf.put("ZEEBE_BROKER_CLUSTER_REPLICATIONFACTOR", "3");
        brokerConf.put("ZEEBE_BROKER_CLUSTER_CLUSTERSIZE", "3");
        brokerConf.put("ZEEBE_BROKER_CLUSTER_INITIALCONTACTPOINTS", "zeebe-broker-0."+ECS_CLUSTER_NAME+":26502, zeebe-broker-1."+ECS_CLUSTER_NAME+":26502, , zeebe-broker-2"+ECS_CLUSTER_NAME+":26502");
        brokerConf.put("ZEEBE_BROKER_GATEWAY_ENABLE", "true");
        brokerConf.put("ZEEBE_LOG_LEVEL", "DEBUG");
        brokerConf.put("ZEEBE_DEBUG", "true");
        brokerConf.put("ATOMIX_LOG_LEVEL", "DEBUG");


        var zeebeContainer = td.addContainer("zeebe-image-"+zeebeNodeId, ContainerDefinitionOptions.builder()
                .cpu(512).memoryLimitMiB(1024)
                .containerName("zeebe")
                .image(ContainerImage.fromRegistry("camunda/zeebe:8.0.0"))
                .portMappings(List.of(
                    PortMapping.builder().containerPort(9600).hostPort(9600).build(),
                    PortMapping.builder().containerPort(26500).hostPort(26500).build(),
                    PortMapping.builder().containerPort(26501).hostPort(26501).build(),
                    PortMapping.builder().containerPort(26502).hostPort(26502).build())
                )
                .environment(brokerConf)
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "zeebe-logs-"+zeebeNodeId)
                                        .logGroupName("/ecs/zeebe-"+zeebeNodeId)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                        .streamPrefix("zeebe-broker-"+zeebeNodeId)
                        .build()))
                .build());

        zeebeContainer.addMountPoints(MountPoint.builder().readOnly(false).containerPath("/usr/local/zeebe/data")
                                                .sourceVolume(dataVolume.getName())
                                                .build());

        return td;
    }


    private FargateTaskDefinition gatewayTaskDefinition() {

        var dataVolume = Volume.builder().name("zeebe-gw-data").build();

        var td = FargateTaskDefinition.Builder.create(this, "zeebe-gw-ecs-task-def")
                .cpu(512)
                .memoryLimitMiB(1024)
                .family("dev-zeebe")
                .volumes(List.of(dataVolume))
                .build();
        td.applyRemovalPolicy(RemovalPolicy.DESTROY);

        var zeebeContainer = td.addContainer("zeebe-image", ContainerDefinitionOptions.builder()
                .cpu(512).memoryLimitMiB(1024)
                .containerName("zeebe")
                .image(ContainerImage.fromRegistry("camunda/zeebe:8.0.0"))
                .portMappings(List.of(
                        PortMapping.builder().containerPort(9600).hostPort(9600).build(),
                        PortMapping.builder().containerPort(26500).hostPort(26500).build(),
                        PortMapping.builder().containerPort(26501).hostPort(26501).build(),
                        PortMapping.builder().containerPort(26502).hostPort(26502).build())
                )
                .environment(
                    Map.of(
                    "JAVA_TOOL_OPTIONS", "-Xms512m -Xmx512m " ,
                    "ZEEBE_STANDALONE_GATEWAY", "true",
                    "ZEEBE_GATEWAY_NETWORK_HOST", "0.0.0.0",
                    "ZEEBE_GATEWAY_NETWORK_PORT", "26500",
                    "ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT", "zeebe-broker-0."+ECS_CLUSTER_NAME+":26502",
                    "ZEEBE_GATEWAY_CLUSTER_PORT", "26502",
                    "ZEEBE_GATEWAY_CLUSTER_HOST", "zeebe-gateway."+ECS_CLUSTER_NAME,
                    "ATOMIX_LOG_LEVEL", "DEBUG"
                    )
                )
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "zeebe-logs")
                                .logGroupName("/ecs/zeebe-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix("zeebe-gateway")
                        .build()))
                .build());

        zeebeContainer.addMountPoints(MountPoint.builder().readOnly(false).containerPath("/usr/local/zeebe/data")
                .sourceVolume(dataVolume.getName())
                .build());

        return td;
    }

}
