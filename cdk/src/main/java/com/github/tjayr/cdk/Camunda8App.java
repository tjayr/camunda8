package com.github.tjayr.cdk;

import com.github.tjayr.cdk.stacks.ZeebeCluster;
import com.github.tjayr.cdk.stacks.ZeebeHazelcastCluster;
import com.github.tjayr.cdk.stacks.ZeebeStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class Camunda8App {
    public static void main(final String[] args) {
        App app = new App();

        var props = StackProps.builder().env(Environment.builder()
                                  .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                                  .region(System.getenv("CDK_DEFAULT_REGION"))
                                  .build())
                              .build();

        new ZeebeStack(app, "zeebe-single", props);
        new ZeebeCluster(app, "zeebe-cluster", props);

        //Run the build-docker.sh before commenting this line in - otherwise the tar file will be missing
        // and this will not compile
        new ZeebeHazelcastCluster(app, "zeebe-cluster-hz", props);

        app.synth();
    }
}

