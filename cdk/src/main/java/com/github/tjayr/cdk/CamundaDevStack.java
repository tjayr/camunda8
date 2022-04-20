package com.github.tjayr.cdk;

import software.amazon.awscdk.DockerBuildOptions;
import software.amazon.awscdk.DockerImage;
import software.amazon.awscdk.services.appmesh.ServiceDiscovery;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.servicediscovery.NamespaceType;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.List;
import java.util.Map;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;

public class CamundaDevStack extends Stack {
    public CamundaDevStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public CamundaDevStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var vpc = Vpc.Builder.create(this, "vpc").maxAzs(1).build();

        var sg = SecurityGroup.Builder.create(this, "sg")
                .allowAllOutbound(true)
                .securityGroupName("camunda-dev-sg")
                .vpc(vpc)
                .build();
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(26500));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(5701));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(9600));
        sg.addIngressRule(Peer.anyIpv4(), Port.tcp(8082));

        var zeebeCluster = Cluster.Builder.create(this, "camunda-cluster")
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder()
                        .name("camunda-dev-cluster")
                        .type(NamespaceType.DNS_PRIVATE)
                        .vpc(vpc)
                        .build())
                .build();

        ApplicationLoadBalancedFargateService.Builder.create(this, "simple-monitor")
                        .cpu(512)
                        .memoryLimitMiB(1024)
                        .desiredCount(1)
                        .cluster(zeebeCluster)
                        .taskImageOptions(ApplicationLoadBalancedTaskImageOptions.builder()
                            .image(ContainerImage.fromRegistry("ghcr.io/camunda-community-hub/zeebe-simple-monitor:2.3.0"))
                        .environment(Map.of(
                                "zeebe.client.broker.gateway-address", "zeebe.camunda-dev-cluster:26500",
                                "zeebe.client.worker.hazelcast.connection", "zeebe.camunda-dev-cluster:5701"
                            )
                        )
                        .build());

        FargateService.Builder.create(this, "zeebe")
                .cluster(zeebeCluster)
                .desiredCount(1)
                .assignPublicIp(false)
                .serviceName("zeebe")
                .taskDefinition(buildZeebeTaskDefinition())
                .build();

    }

    private TaskDefinition buildZeebeTaskDefinition() {
        var td = TaskDefinition.Builder.create(this, "zeebe-ecs-task-def")
                .cpu("512")
                .memoryMiB("1024")
                .family("dev-zeebe")
                .build();


//        image: camunda/zeebe:${CAMUNDA_PLATFORM_VERSION:-8.0.0}
//        container_name: zeebe
//        ports:
//        - "26500:26500"
//                - "9600:9600"
//                - "5701:5701"
//        environment:
//        - ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_CLASSNAME=io.camunda.zeebe.exporter.ElasticsearchExporter
//                - ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_URL=http://elasticsearch:9200
//        - ZEEBE_BROKER_EXPORTERS_ELASTICSEARCH_ARGS_BULK_SIZE=1
//                - ZEEBE_BROKER_DATA_DISKUSAGECOMMANDWATERMARK=0.998
//                - ZEEBE_BROKER_DATA_DISKUSAGEREPLICATIONWATERMARK=0.999
//                - "JAVA_TOOL_OPTIONS=-Xms512m -Xmx512m"
//        restart: always
//        volumes:
//        - zeebe:/usr/local/zeebe/data
//                - ./zeebe-hazelcast-exporter-1.1.0-jar-with-dependencies.jar:/usr/local/zeebe/exporters/zeebe-hazelcast-exporter.jar
//                - ./application.yaml:/usr/local/zeebe/config/application.yaml
//        networks:
//        - camunda-platform
//        depends_on:
//        - elasticsearch

//
//        var zeebeImage = ContainerImage.fromDockerImageAsset(
//                DockerImageAsset.Builder.create(this, "").directory("").
//                .build());

        td.addContainer("zeebe-image", ContainerDefinitionOptions.builder()
                .cpu(512).memoryLimitMiB(1024)
                .containerName("zeebe")
                .image(ContainerImage.fromRegistry("camunda/zeebe:8.0.0"))
                .portMappings(List.of(
                    PortMapping.builder().containerPort(9600).hostPort(9600).build(),
                    PortMapping.builder().containerPort(5701).hostPort(5701).build(),
                    PortMapping.builder().containerPort(26500).hostPort(26500).build())
                )
                .environment(
                    Map.of(
                        "JAVA_TOOL_OPTIONS", "-Xms512m -Xmx512m",
                        "ZEEBE_BROKER_DATA_DISKUSAGECOMMANDWATERMARK", "0.998",
                        "ZEEBE_BROKER_DATA_DISKUSAGEREPLICATIONWATERMARK", "0.999"
                    )
                )
                .build());

        return td;
    }
}
