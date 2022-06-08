package com.github.tjayr.cdk.constructs;

import org.jetbrains.annotations.NotNull;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.servicediscovery.DnsRecordType;
import software.amazon.awscdk.services.servicediscovery.INamespace;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;
import software.amazon.awscdk.services.servicediscovery.PrivateDnsNamespace;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZeebeCluster extends Construct {

    private ZeebeClusterOptions options;
    private INamespace defaultNamespace;
    private IVpc targetVpc;
    private ICluster targetCluster;

    public ZeebeCluster(@NotNull Construct scope, @NotNull String id, ZeebeClusterOptions zeebeClusterOptions) {
        super(scope, id);
        this.options = zeebeClusterOptions;

        if(options.getVpc() == null) {
            targetVpc = Vpc.fromLookup(this, "", VpcLookupOptions.builder().isDefault(true).build());
        }

        if(options.getNamespace() == null) {
            defaultNamespace = PrivateDnsNamespace.Builder.create(this, "zeebe-default-ns")
                    .name("zeebe-cluster.net").description("Zeebe Cluster Namespace")
                    .vpc(targetVpc)
                    .build();
        }

        if(options.getCluster() == null) {
            targetCluster = Cluster.Builder.create(this, "zeebe-cluster")
                    .clusterName("zeebe-cluster")
                    .vpc(targetVpc)
                    .build();
        }
    }

    private FargateService createGateway() {

        var fservice = FargateService.Builder.create(this, "zeebe-gateway")
                .desiredCount(options.getBrokerNodes())
                .minHealthyPercent(100).maxHealthyPercent(200)
                .deploymentController(DeploymentController.builder().type(DeploymentControllerType.ECS).build())
                .serviceName("gateway")
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .taskDefinition(gatewayTaskDefinition());


        if(options.getCluster() == null) {
            fservice.cluster(targetCluster);
        } else {
            fservice.cluster(options.getCluster());
        }

        if(options.getSecurityGroup() == null) {
            //create security group
        } else {
            fservice.securityGroups(List.of(options.getSecurityGroup()));
        }

        if(options.getLoadBalancer() == null) {
            fservice.vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build());
            fservice.assignPublicIp(true);
        } else {
            fservice.assignPublicIp(true);
        }

        if(options.getNamespace() == null) {
            fservice.cloudMapOptions(CloudMapOptions.builder().name("gateway")
                    .cloudMapNamespace(defaultNamespace)
                    .dnsRecordType(DnsRecordType.A)
                    .build());
        } else {
            fservice.cloudMapOptions(CloudMapOptions.builder().name("gateway")
                    .cloudMapNamespace(options.getNamespace())
                    .dnsRecordType(DnsRecordType.A)
                    .build());
        }


        return fservice.build();
    }

    private FargateTaskDefinition gatewayTaskDefinition() {

        var dataVolume = Volume.builder().name("zeebe-gw-data").build();

        var td = FargateTaskDefinition.Builder.create(this, "zeebe-gw-ecs-task-def")
                .cpu(options.getGatewayCpu())
                .memoryLimitMiB(options.getGatewayMemory())
                .family("zeebe")
                .volumes(List.of(dataVolume))
                .build();
        td.applyRemovalPolicy(RemovalPolicy.DESTROY);

        var zeebeContainer = td.addContainer("zeebe-gw", ContainerDefinitionOptions.builder()
                .cpu(512).memoryLimitMiB(1024)
                .containerName("zeebe-gw")
                .image(ContainerImage.fromRegistry("camunda/zeebe:8.0.2"))
                .portMappings(List.of(
                        PortMapping.builder().containerPort(9600).hostPort(9600).build(),
                        PortMapping.builder().containerPort(26500).hostPort(26500).build(),
                        PortMapping.builder().containerPort(26501).hostPort(26501).build(),
                        PortMapping.builder().containerPort(26502).hostPort(26502).build())
                )
                .environment(buildGatewayEnvironment())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "zeebe-logs")
                                .logGroupName("/ecs/zeebe-gateway")
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .retention(RetentionDays.ONE_MONTH)
                                .build())
                        .streamPrefix("zeebe-gateway")
                        .build()))
                .build());

        zeebeContainer.addMountPoints(MountPoint.builder().readOnly(false).containerPath("/usr/local/zeebe/data")
                .sourceVolume(dataVolume.getName())
                .build());

        return td;
    }

    private Map<String, String> buildGatewayEnvironment() {
        var env = Map.of(
                "JAVA_TOOL_OPTIONS", "-Xms512m -Xmx512m " ,
                "ZEEBE_STANDALONE_GATEWAY", "true",
                "ZEEBE_GATEWAY_NETWORK_HOST", "0.0.0.0",
                "ZEEBE_GATEWAY_NETWORK_PORT", "26500",
                "ZEEBE_GATEWAY_CLUSTER_CONTACTPOINT", "zeebe-broker-0."+options.getNamespace().getNamespaceName()+":26502",
                "ZEEBE_GATEWAY_CLUSTER_PORT", "26502",
                "ZEEBE_GATEWAY_CLUSTER_HOST", "zeebe-gateway."+options.getNamespace().getNamespaceName(),
                "ZEEBE_BROKER_GATEWAY_ENABLE", "true",
                "ATOMIX_LOG_LEVEL", "DEBUG"
        );
        var environment = new HashMap<>(env);
        environment.putAll(options.getGatewayEnvironment());
        return environment;
    }

    private String createZeebeContactPoints(int clusterSize, int port) {
        StringBuilder bldr = new StringBuilder();
        for(int i=0; i < clusterSize; i++) {
            bldr.append("zeebe-broker-").append(i)
                    .append(".").append(options.getNamespace().getNamespaceName())
                    .append(":").append(port);

            if(i < clusterSize - 1) {
                bldr.append(", ");
            }
        }
        return bldr.toString();
    }
}
