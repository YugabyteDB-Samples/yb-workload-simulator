## What is yb-simulation-base-demo-app
This is our base app containing some generic workloads. You can create/drop tables for a workload, load data, run different simulations all from UI. Web page provides you with live latency and throughput about your yugabyte db cluster while these simulations are running. This repo acts as a base and you can extend it to write your specific simulations. I will be providing some starter projects/instructions for that soon. 

## Code setup and Installation

### How to build the jar file
Go to the root of the project and run:
```
mvn clean package -DskipTests
```
This will create the jar file: <yb-simulation-base-demo-app>/target/yb-simulation-base-app.jar

### How to run this App

Here is the basic command. You can pass additional parameters as needed:

```
java -DXmx=16g -Dmax-pool-size=10 -Dnode=<database-ip-or-name> -Ddbuser=<db-user-id> -Ddbpassword=<db-password> -Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone> -Dspring.workload=genericWorkload -jar yb-simu-base-app.jar
```

#### Parameters you can add to above java command: 
```
-Dnode=<database-host-name> [default: 127.0.0.1]
-Ddbuser=<userid> [default: yugabyte]
-Ddbpassword=<password> [default: yugabyte]
-Dport=<port> [default: 5433 - database port if using other than 5433]
-Dmax-pool-size=<max-pool-size> [default: 100]
-Ddbname=<dbname> [default: yugabyte]
-Dworkload=<workload-id> [ default: genericWorkload ]
-Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone> [ex. aws.us-east-2.us-east-2a,aws.us-east-2.us-east-2b,aws.us-east-2.us-east-2c]
-Dspring.profiles.active=<profile> [default: application.yaml]
-Dserver.port=<app-ui-port> [default: 8080]
-Dssl=true [default: false]
-Dsslmode=verify-full [default: disable]
-Dsslrootcert=<certificatepath> 
```

#### Additional parameters if you wish to run YCQL workload
```
-Dworkload=genericCassandraWorkload
-Dspring.data.cassandra.contact-points=<host ip> 
-Dspring.data.cassandra.port=9042 
-Dspring.data.cassandra.local-datacenter=<datacenter> [ex. us-east-2 ]
-Dspring.data.cassandra.userid=cassandra 
-Dspring.data.cassandra.password=<cassandra-password>
```

#### Local Environment: 
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Prod APP UI: 
```
http://<HOSTNAME>:8080
```
