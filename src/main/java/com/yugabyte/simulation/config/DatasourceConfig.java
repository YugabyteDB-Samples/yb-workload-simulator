package com.yugabyte.simulation.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatasourceConfig {
//	@Bean
//	@ConfigurationProperties("app.datasource")
//	public HikariDataSource dataSource() {
//		DataSourceProperties props  = new DataSourceProperties();
//	    return (HikariDataSource) DataSourceBuilder.create()
//	            .type(HikariDataSource.class)
//	            .build();
//	}
	
    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.hikari")
    public HikariConfig hikariConfig() {
        //return new HikariConfig();
    	HikariConfig config = new HikariConfig();
//    	Properties properties = new Properties();
//    	properties.setProperty(PGProperty.PREFER_QUERY_MODE.getName(), PreferQueryMode.SIMPLE.value());
//    	config.setDataSourceProperties(properties);
    	
//    	Properties props = config.getDataSourceProperties();
    	//props.setProperty(PGProperty.PREFER_QUERY_MODE.getName(), PreferQueryMode.EXTENDED_CACHE_EVERYTHING.value());
//    	config.setDataSourceProperties(props);
    	return config;
    }

    @Bean
    @Primary // may not be required
    public DataSource dataSource() {
//    	Properties properties = 
    	HikariDataSource ds = new HikariDataSource(hikariConfig());
//    	ds.addDataSourceProperty(PGProperty.PREFER_QUERY_MODE.getName(), PreferQueryMode.EXTENDED_CACHE_EVERYTHING);
////        return new HikariDataSource(hikariConfig());
    	return ds;
    }
}