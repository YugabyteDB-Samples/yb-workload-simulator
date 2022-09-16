package com.yugabyte.simulation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yugabyte.simulation.service.GenericCassandraWorkload;
import com.yugabyte.simulation.service.GenericWorkload;
import com.yugabyte.simulation.service.NewFormatWorkload;
import com.yugabyte.simulation.service.PitrSqlDemoWorkload;
import com.yugabyte.simulation.service.SimpleSelectWorkload;
import com.yugabyte.simulation.service.WorkloadSimulation;

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

}
