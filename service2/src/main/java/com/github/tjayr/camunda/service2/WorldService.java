package com.github.tjayr.camunda.service2;

import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.worker.JobClient;
import io.camunda.zeebe.spring.client.annotation.ZeebeWorker;
import org.springframework.stereotype.Service;

@Service
public class WorldService {

    @ZeebeWorker(type = "world")
    public void handleJobFoo(final JobClient client, final ActivatedJob job) {

        System.out.println("World");

        client.newCompleteCommand(job.getKey())
                .send()
                .exceptionally( throwable -> { throw new RuntimeException("Could not complete job " + job, throwable); });
    }

}
