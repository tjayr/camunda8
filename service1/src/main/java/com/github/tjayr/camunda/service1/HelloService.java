package com.github.tjayr.camunda.service1;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class HelloService {

    @ZeebeWorker(type = "hello")
    public void handleJobFoo(final JobClient client, final ActivatedJob job) {

        System.out.println("Hello vars "+job.getVariablesAsMap());

        client.newCompleteCommand(job.getKey()).variables(Map.of()).send().exceptionally( throwable -> { throw new RuntimeException("Could not complete job " + job, throwable); });
        client.newCompleteCommand(job.getKey())
                .send()
                .exceptionally( throwable -> { throw new RuntimeException("Could not complete job " + job, throwable); });
    }


}
