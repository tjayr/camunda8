package com.github.tjayr.cdk.constructs;

import org.jetbrains.annotations.NotNull;
import software.constructs.Construct;

public class ZeebeFargateNode  extends Construct {

    private ZeebeFargateNodeOptions options;

    public ZeebeFargateNode(@NotNull Construct scope, @NotNull String id, ZeebeFargateNodeOptions options) {
        super(scope, id);
    }

}
