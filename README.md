# YB Workload Simulator application

YB Workload Simulator is a Java application that simulates workloads against YugabyteDB and provides live metrics of latency and throughput from the application's point of view. You can create/drop tables for a workload, load data, run different simulations from the application UI. You can view latency and throughput metrics in real time for your YugabyteDB cluster when the simulations are running. This repository acts as a base and you can extend it to write your specific simulations.

## Table of contents

* [Download the jar](#download-the-jar)
   * [Run the application locally](#run-the-application-locally)
   * [Run the application on a YugabyteDB Managed cluster](#run-the-application-on-a-yugabytedb-managed-cluster)
* [Code setup and Installation](#code-setup-and-installation)
   * [Build the jar file](#build-the-jar-file)
      * [Additional parameters for YCQL workloads](#additional-parameters-for-ycql-workloads)
* [Start a read and write workload](#start-a-read-and-write-workload)
* [Create your own workload](#create-your-own-workload)

## Download the jar

YB Workload Simulator requires Java 19 or later installed on your computer. JDK installers for Linux and macOS can be downloaded from [Oracle](http://jdk.java.net/), [Adoptium (OpenJDK)](https://adoptium.net/), or [Azul Systems (OpenJDK)](https://www.azul.com/downloads/?package=jdk). Homebrew users on macOS can install using `brew install openjdk`.

Download the latest YB Workload Simulator JAR file from the [releases](https://github.com/YugabyteDB-Samples/yb-workload-simulator/releases) page.

### Run the application locally

To run the application locally, do the following:

1. Install a local YugabyteDB cluster. Refer to [Set up your YugabyteDB cluster](https://docs.yugabyte.com/preview/explore/#set-up-your-yugabytedb-cluster) to start a local 3 node custer.

1. To start the application against a running local cluster, use the following command:

    ```sh
    java -jar ./yb-workload-sim-0.0.9.jar
    ```

    By default, the application connects to a local YugabyteDB cluster at 127.0.0.1.

    To connect to a different address or node, use the `-Dnode` flag to specify an IP address. For example:

    ```sh
    java -Dnode=127.0.0.2 -jar ./yb-workload-sim-0.0.2.jar
    ```

1. To view the application UI, navigate to `http://localhost:8080`.

   You can pass additional parameters as needed:

    ```sh
    java -DXmx=16g
         -Dmax-pool-size=10
         -Dnode=<database-ip-or-name>
         -Ddbuser=<db-user-id>
         -Ddbpassword=<db-password>
         -Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone>
         -Dspring.workload=genericWorkload
         -jar yb-workload-sim-0.0.2.jar
    ```

    The parameters you can add to the java command are as follows:

    ```sh
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

### Run the application using postgres driver
By default, workload simulator uses "Yugabyte Smart Driver". You can run the App using "Postgres Driver" by passing following flag:
```
-Dspring.profiles.active=pgdriver
```

### Run the application on a YugabyteDB Managed cluster (Aeon)
YugabyteDB smart driver doesn't connect to YugabyteDB Aeon cluster if you are running your app from your laptop. You will need to run the app from a machine which has VPC peered with Yugabyte Aeon VPC. 

#### Workaround:
If you need to run app against YugabyteDB Aeon on laptop, you can use the postgres driver. Please see "Run the application using postgres driver"


## Code setup and Installation

### Build the jar file

From the root of the project run the following maven command:

```sh
mvn clean package -DskipTests
```

A jar file gets created at : <yb-workload-sim>/target/yb-workload-simulator.jar. You can [run the application locally](https://github.com/YugabyteDB-Samples/yb-workload-simulator/edit/main/README.md#run-the-application-locally) using [YugabyteDB Managed](#run-the-application-on-a-yugabytedb-managed-cluster) using the jar.

#### Additional parameters for YCQL workloads

```sh
-Dworkload=genericCassandraWorkload
-Dspring.data.cassandra.contact-points=<host ip>
-Dspring.data.cassandra.port=9042
-Dspring.data.cassandra.local-datacenter=<datacenter> [ex. us-east-2 ]
-Dspring.data.cassandra.userid=cassandra
-Dspring.data.cassandra.password=<cassandra-password>
-Dspring.data.cassandra.sslcertpath=<path-to-root-cert> [ex. /Users/username/root.crt]  
```


## Start a read and write workload

To view the application UI, navigate to <http://localhost:8080>

1. In the application UI, click the hamburger icon at the top of the page beside **Active Workloads**.

1. Select the **Usable Operations** tab.

1. Under **Create Tables**, click **Run Create Tables Workload** to add tables to the database.

1. Under **Seed Data**, click **Run Seed Data Workload** to add data to the tables.

1. Under **Simulation**, select the **Include new Inserts** option, and click **Run Simulation Workload**.

1. Click **Close**.

The Latency and Throughput charts show the workload running on the cluster.

## Create your own workload
It is very easy to bring in your Data Model and run simulations against it.
1. Navigate to following directory: src/main/java/com/yugabyte/simulation/service
2. Clone one of existing workloads (ex. QuikShipWorkload) and create your workload java file (example: MyAwesomeWorkload.java ).
3. In this new java file, add your DDL and simulation you wish to run.
4. Add a new entry for this new workload in WorkloadConfig.java file
```sh
   @Bean(name="MyAwesomeWorkload")
   public WorkloadSimulation myAwesomeWorkload(){
   return new MyAwesomeWorkload();
   }
   ```
5. When running the workload override the default workload
```sh
-Dspring.workload=MyAwesomeWorkload
```

## App UI
After starting your app, you can access the UI:
```
http://<host>:8080
```

1. Start the simulations by bringing the popup from left side hamburger menu 
<img width="875" height="664" alt="Screenshot 2025-08-21 at 12 39 42 AM" src="https://github.com/user-attachments/assets/d802bb02-ee6b-4ef4-9b0e-dbc4d3112542" />

2. This is how your UI will look like after simulations start. You can run multiple simulations in parallel.
<img width="1915" height="867" alt="Screenshot 2025-08-21 at 12 41 07 AM" src="https://github.com/user-attachments/assets/cd478b04-1510-4082-b6d5-2c39a4ccc786" />


