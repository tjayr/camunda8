package com.github.tjayr.client;

import io.grpc.*;

public class TenantProviderInterceptor implements ClientInterceptor  {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> methodDescriptor, final CallOptions callOptions, final Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(final Listener<RespT> responseListener, final Metadata headers) {
                headers.put(Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER), "mytoken");
                headers.put(Metadata.Key.of("customerId", Metadata.ASCII_STRING_MARSHALLER), "dcu-test");
                super.start(responseListener, headers);
            }
        };
    }

}
