package com.github.tjayr.camunda.service1;

import com.github.tjayr.client.TenantProviderInterceptor;
import io.camunda.zeebe.spring.client.EnableZeebeClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeDeployment;
import io.grpc.ClientInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
@EnableZeebeClient
@ZeebeDeployment(resources = { "classpath:hello-world.bpmn", "classpath:hello-world-dmn.bpmn", "classpath:status-decision.dmn"})
public class Service1Application {

    @Bean
    List<ClientInterceptor> clientInterceptorList() {
        return List.of(new TenantProviderInterceptor());
    }

    @Autowired
    private HelloService helloService;

    public static void main(String[] args) {
        SpringApplication.run(Service1Application.class, args);
    }

}
