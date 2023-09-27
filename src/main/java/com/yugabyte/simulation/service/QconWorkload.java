package com.yugabyte.simulation.service;

import com.yugabyte.simulation.dao.InvocationResult;
import com.yugabyte.simulation.dao.ParamValue;
import com.yugabyte.simulation.dao.WorkloadDesc;
import com.yugabyte.simulation.dao.WorkloadParamDesc;
import com.yugabyte.simulation.services.ServiceManager;
import com.yugabyte.simulation.workload.FixedTargetWorkloadType;
import com.yugabyte.simulation.workload.WorkloadSimulationBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Repository
public class QconWorkload extends WorkloadSimulationBase implements WorkloadSimulation{


    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ServiceManager serviceManager;

    @Value("${SPRING_APPLICATION_NAME:}")
    private String applicationName;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Override
    public String getName() {
        return "QCon"+ ((applicationName != null && !applicationName.equals(""))? " ["+applicationName+"]" : "");
    }

    private enum WorkloadType {
        SEED_DATA
    }

    private final FixedTargetWorkloadType seedingWorkloadType;

    public QconWorkload() {
        this.seedingWorkloadType = new FixedTargetWorkloadType();
    }


    private WorkloadDesc seedingWorkload = new WorkloadDesc(
            QconWorkload.WorkloadType.SEED_DATA.toString(),
            "API Call",
            "Load data into the database",
            new WorkloadParamDesc("API call #:", 1, Integer.MAX_VALUE, 10000),
            new WorkloadParamDesc("Threads", 1, Integer.MAX_VALUE, 32),
            new WorkloadParamDesc("API Endpoint", "http://localhost:8080/transactions/create-random-transaction")
    );



    @Override
    public List<WorkloadDesc> getWorkloads() {
        return Arrays.asList(
                seedingWorkload
        );
    }


    @Override
    public InvocationResult invokeWorkload(String workloadId, ParamValue[] values) {
        QconWorkload.WorkloadType type = QconWorkload.WorkloadType.valueOf(workloadId);
        try {
            switch (type) {
                case SEED_DATA:
                    this.seedData(values[0].getIntValue(), values[1].getIntValue(),values[2].getStringValue() );
                    return new InvocationResult("Ok");
            }
            throw new IllegalArgumentException("Unknown workload "+ workloadId);
        }
        catch (Exception e) {
            return new InvocationResult(e);
        }
    }


    private void seedData(int numberToGenerate, int threads,String apiEndpoint) {
        seedingWorkloadType
                .createInstance(serviceManager)
                .execute(threads, numberToGenerate, (customData, threadData) -> {
                    makeAPICall(apiEndpoint);
                    return threadData;
                });
    }

    private void makeAPICall(String apiEndpoint){
        // Make API call
        try {
            // Create a WebClient instance using the injected builder
            WebClient webClient = webClientBuilder.baseUrl(apiEndpoint).build();

            // Make a synchronous GET request using block
            String responseBody = webClient.get().retrieve().bodyToMono(String.class).block();

            // Process the response
            System.out.println("API Response: " + responseBody);
        } catch (Exception e) {
            // Handle exceptions
            System.err.println("API Call failed: " + e.getMessage());
        }
    }




}
