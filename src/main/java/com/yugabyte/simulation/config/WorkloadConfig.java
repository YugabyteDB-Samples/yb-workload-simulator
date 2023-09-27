package com.yugabyte.simulation.config;

import com.yugabyte.simulation.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkloadConfig {
    @Bean(name="SimpleSelectWorkload")
    public WorkloadSimulation simpleSelectWorkload(){
        return new SimpleSelectWorkload();
    }

    @Bean(name="PitrDemoWorkload")
    public WorkloadSimulation pitrDemoWorkload(){
        return new PitrSqlDemoWorkload();
    }

    @Bean(name="NewFormatWorkload")
    public WorkloadSimulation newFormatWorkload(){
        return new NewFormatWorkload();
    }

    @Bean(name="GenericWorkload")
    public WorkloadSimulation genericWorkload(){
        return new GenericWorkload();
    }

    @Bean(name="GenericCassandraWorkload")
    public WorkloadSimulation genericCassandraWorkload(){
        return new GenericCassandraWorkload();
    }

    @Bean(name="QuikShipWorkload")
    public WorkloadSimulation quikShipWorkload(){
        return new QuikShipWorkload();
    }

    @Bean(name="QconWorkload")
    public WorkloadSimulation qconWorkload(){
        return new QconWorkload();
    }
}
