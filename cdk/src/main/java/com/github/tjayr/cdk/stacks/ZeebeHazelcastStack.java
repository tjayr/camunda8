package com.github.tjayr.cdk.stacks;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Volume;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.efs.*;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ZeebeHazelcastStack extends Stack {

    public ZeebeHazelcastStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ZeebeHazelcastStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var vpc = Vpc.fromLookup(this, "default-vpc", VpcLookupOptions.builder().isDefault(true).build());

        var sg = SecurityGroup.Builder.create(this, "zb-hc-sg")
                .allowAllOutbound(true)
                .securityGroupName("zeebe-hazelcast-dev-sg")
                .vpc(vpc)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(26500));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(26501));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(26502));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(5701));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(9600));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(2049));
        sg.applyRemovalPolicy(RemovalPolicy.DESTROY);

        var efs = FileSystem.Builder.create(this, "efs")
                                    .vpc(vpc)
                                    .encrypted(false)
                                    .fileSystemName("zeebe-efs")
                                    .enableAutomaticBackups(false)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .performanceMode(PerformanceMode.GENERAL_PURPOSE)
                                    .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                                    .lifecyclePolicy(LifecyclePolicy.AFTER_14_DAYS)
                                    .securityGroup(sg)
                                    .build();

        efs.addAccessPoint("data", AccessPointOptions.builder().posixUser(PosixUser.builder()
                                    .uid("1001").gid("1001").build())
                                    .path("/").build());

        var zeebeCluster = Cluster.Builder.create(this, "zeebe-cluster")
                .clusterName("zeebe-cluster")
                .vpc(vpc)
                .build();

        FargateService.Builder.create(this, "zeebe")
                .cluster(zeebeCluster)
                .desiredCount(1)
                .minHealthyPercent(100).maxHealthyPercent(200)
                .deploymentController(DeploymentController.builder().type(DeploymentControllerType.ECS).build())
                .assignPublicIp(true)
                .serviceName("zeebe")
                .taskDefinition(buildZeebeTaskDefinition(efs.getFileSystemId()))
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
                .build();
    }

    private TaskDefinition buildZeebeTaskDefinition(String fileSystemId) {

        var efsConfig = EfsVolumeConfiguration.builder()
                .rootDirectory("/")
                .fileSystemId(fileSystemId).build();

        var efsVolume = Volume.builder().name("zeebe-data")
                .efsVolumeConfiguration(efsConfig).build();

        var td = TaskDefinition.Builder.create(this, "zeebe-hazelcast-ecs-task-def")
                .cpu("512")
                .memoryMiB("1024")
                .family("dev-zeebe")
                .volumes(List.of(efsVolume))
                .compatibility(Compatibility.FARGATE)
                .build();
        td.applyRemovalPolicy(RemovalPolicy.DESTROY);

        var zeebeContainer = td.addContainer("zeebe-hazelcast-image", ContainerDefinitionOptions.builder()
                .cpu(512).memoryLimitMiB(1024)
                .containerName("zeebe")
                .image(ContainerImage.fromTarball("zeebe-hazelcast.tar.gz"))
                .portMappings(List.of(
                    PortMapping.builder().containerPort(9600).hostPort(9600).build(),
                    PortMapping.builder().containerPort(26500).hostPort(26500).build(),
                    PortMapping.builder().containerPort(26501).hostPort(26501).build(),
                    PortMapping.builder().containerPort(26501).hostPort(26501).build(),
                    PortMapping.builder().containerPort(26502).hostPort(26502).build(),
                    PortMapping.builder().containerPort(5701).hostPort(5701).build())
                )
                .environment(
                    Map.of(
                        "JAVA_TOOL_OPTIONS", "-Xms512m -Xmx512m " +
                                                     "-Dzeebe.broker.exporters.hazelcast.className=io.zeebe.hazelcast.exporter.HazelcastExporter -Dzeebe.broker.exporters.hazelcast.jarPath=exporters/zeebe-hazelcast-exporter.jar",
                        "ZEEBE_BROKER_DATA_DISKUSAGECOMMANDWATERMARK", "0.998",
                        "ZEEBE_BROKER_DATA_DISKUSAGEREPLICATIONWATERMARK", "0.999",
                        "ZEEBE_BROKER_NETWORK_HOST", "0.0.0.0",
                        "ZEEBE_BROKER_GATEWAY_CLUSTER_HOST","0.0.0.0"
                    )
                )
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, "zeebe-logs")
                                        .logGroupName("/ecs/zeebe")
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                        .streamPrefix("zeebe")
                        .build()))
                .build());

        zeebeContainer.addMountPoints(MountPoint.builder().readOnly(false).containerPath("/usr/local/zeebe/data")
                                                .sourceVolume(efsVolume.getName())
                                                .build());

        return td;
    }

}
