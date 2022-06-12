package com.github.tjayr.server;

import io.camunda.zeebe.gateway.protocol.GatewayOuterClass;
import io.grpc.ServerInterceptor;
import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZeebeTenantInterceptor implements ServerInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZeebeTenantInterceptor.class);

    public ZeebeTenantInterceptor() {}

    @Override
    public <ReqT, RespT> Listener<ReqT> interceptCall(final ServerCall<ReqT, RespT> call, final Metadata headers,
            final ServerCallHandler<ReqT, RespT> next) {

        final var listener = next.startCall(call, headers);

        return new SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(final ReqT message) {

                var token = headers.get(Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER));
                var cid = headers.get(Metadata.Key.of("customerId", Metadata.ASCII_STRING_MARSHALLER));

                LOGGER.info("intercepted - token="+token +", cid="+cid);
                super.onMessage(message);
            }
        };
    }
}
