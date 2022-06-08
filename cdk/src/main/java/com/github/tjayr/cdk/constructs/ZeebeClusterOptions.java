package com.github.tjayr.cdk.constructs;

import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.efs.FileSystem;
import software.amazon.awscdk.services.elasticloadbalancingv2.IApplicationLoadBalancer;
import software.amazon.awscdk.services.servicediscovery.INamespace;

import java.util.Map;

public class ZeebeClusterOptions {

    private ICluster cluster;
    private IVpc vpc;
    private int gatewayNodes = 1;
    private int brokerNodes = 3;
    private FileSystem efs;
    private Map<String, String> brokerEnvironment;
    private Map<String, String> gatewayEnvironment;
    private IApplicationLoadBalancer loadBalancer;
    private SecurityGroup securityGroup;
    private INamespace namespace;
    private Number gatewayCpu;
    private Number gatewayMemory;
    private Number brokerMemory;
    private Number brokerCpu;

    private ZeebeClusterOptions(ICluster cluster, IVpc vpc, int gatewayNodes, int brokerNodes, FileSystem efs, Map<String, String> brokerEnvironment, Map<String, String> gatewayEnvironment, IApplicationLoadBalancer loadBalancer, SecurityGroup securityGroup, INamespace namespace, Number gatewayCpu, Number gatewayMemory, Number brokerMemory, Number brokerCpu) {
        this.cluster = cluster;
        this.vpc = vpc;
        this.gatewayNodes = gatewayNodes;
        this.brokerNodes = brokerNodes;
        this.efs = efs;
        this.brokerEnvironment = brokerEnvironment;
        this.gatewayEnvironment = gatewayEnvironment;
        this.loadBalancer = loadBalancer;
        this.securityGroup = securityGroup;
        this.namespace = namespace;
        this.gatewayCpu = gatewayCpu;
        this.gatewayMemory = gatewayMemory;
        this.brokerMemory = brokerMemory;
        this.brokerCpu = brokerCpu;
    }

    public INamespace getNamespace() {
        return namespace;
    }

    public IVpc getVpc() {
        return vpc;
    }

    public int getGatewayNodes() {
        return gatewayNodes;
    }

    public int getBrokerNodes() {
        return brokerNodes;
    }

    public FileSystem getEfs() {
        return efs;
    }

    public Map<String, String> getBrokerEnvironment() {
        return brokerEnvironment;
    }

    public Map<String, String> getGatewayEnvironment() {
        return gatewayEnvironment;
    }


    public Builder builder() {
        return new Builder();
    }

    public IApplicationLoadBalancer getLoadBalancer() {
        return loadBalancer;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public ICluster getCluster() {
        return cluster;
    }

    public Number getGatewayCpu() {
        return gatewayCpu;
    }

    public Number getGatewayMemory() {
        return gatewayMemory;
    }

    public Number getBrokerMemory() {
        return brokerMemory;
    }

    public Number getBrokerCpu() {
        return brokerCpu;
    }

    public static final class Builder {
        private IVpc vpc;
        private int gatewayNodes = 1;
        private int brokerNodes = 3;
        private FileSystem efs;
        private Map<String, String> brokerEnvironment;
        private Map<String, String> gatewayEnvironment;
        private IApplicationLoadBalancer loadBalancer;
        private ICluster cluster;
        private SecurityGroup securityGroup;
        private INamespace namespace;
        private Number gatewayCpu = 512;
        private Number gatewayMemory = 1024;
        private Number brokerMemory = 1024;
        private Number brokerCpu = 512;

        public Builder brokerCpu(Number cpu) {
            this.brokerCpu = cpu;
            return this;
        }
        public Builder brokerMemory(Number mem) {
            this.brokerMemory = mem;
            return this;
        }

        public Builder gatewayCpu(Number cpu) {
            this.gatewayCpu = cpu;
            return this;
        }
        public Builder gatewayMemory(Number mem) {
            this.gatewayMemory = mem;
            return this;
        }

        public Builder namespace(INamespace ns) {
            this.namespace = ns;
            return this;
        }

        public Builder securityGroup(SecurityGroup securityGroup) {
            this.cluster = cluster;
            return this;
        }

        public Builder vpc(ICluster cluster) {
            this.cluster = cluster;
            return this;
        }

        public Builder vpc(IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public Builder gatewayNodes(int numberOfGateways) {
            this.gatewayNodes = numberOfGateways;
            return this;
        }

        public Builder brokerNodes(int numberOfBrokers) {
            this.gatewayNodes = numberOfBrokers;
            return this;
        }

        public Builder brokerConfig(Map<String, String> environment) {
            this.brokerEnvironment = environment;
            return this;
        }

        public Builder gatewayConfig(Map<String, String> environment) {
            this.gatewayEnvironment = environment;
            return this;
        }

        public Builder withFileSystem(FileSystem fileSystem) {
            this.efs = fileSystem;
            return this;
        }

        public Builder gatewayLoadbalancer(IApplicationLoadBalancer alb) {
            this.loadBalancer = alb;
            return this;
        }

        public ZeebeClusterOptions build() {
            return new ZeebeClusterOptions(cluster, vpc, gatewayNodes, brokerNodes, efs, brokerEnvironment,
                    gatewayEnvironment, loadBalancer, securityGroup, namespace, gatewayCpu, gatewayMemory, brokerMemory, brokerCpu);
        }

    }
}
