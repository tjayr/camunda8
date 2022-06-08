package com.github.tjayr.cdk.constructs;

import java.util.Map;

public class ZeebeFargateNodeOptions {

    private String nodeId;
    private Map<String,String> environmentVariables;
    private boolean gateway;

    public ZeebeFargateNodeOptions(boolean gateway, String nodeId, Map<String, String> environmentVariables) {
        this.environmentVariables = environmentVariables;
        this.gateway=gateway;
        this.nodeId=nodeId;
    }

}
