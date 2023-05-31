# YB Workload Simulator application

YB Workload Simulator is a Java application that simulates workloads against YugabyteDB and provides live metrics of latency and throughput from the application's point of view. You can create/drop tables for a workload, load data, run different simulations from the application UI. You can view latency and throughput metrics in real time for your YugabyteDB cluster when the simulations are running. This repository acts as a base and you can extend it to write your specific simulations.

## Table of contents

* [Download the jar](#download-the-jar)
   * [Run the application locally](#run-the-application-locally)
   * [Run the application on a YugabyteDB Managed cluster](#run-the-application-on-a-yugabytedb-managed-cluster)
* [Code setup and Installation](#code-setup-and-installation)
   * [Build the jar file](#build-the-jar-file)
      * [Additional parameters for YCQL workloads](#additional-parameters-for-ycql-workloads)
* [How to build your own workload](#how-to-build-your-own-workload)
* [Start a read and write workload](#start-a-read-and-write-workload)
* [Create your own workload .java file](#create-your-own-workload-java-file)

## Download the jar

YB Workload Simulator requires Java 19 or later installed on your computer. JDK installers for Linux and macOS can be downloaded from [Oracle](http://jdk.java.net/), [Adoptium (OpenJDK)](https://adoptium.net/), or [Azul Systems (OpenJDK)](https://www.azul.com/downloads/?package=jdk). Homebrew users on macOS can install using `brew install openjdk`.

Download the latest YB Workload Simulator JAR file from the [releases](https://github.com/YugabyteDB-Samples/yb-workload-simulator/releases) page.

### Run the application locally

To run the application locally, do the following:

1. Install a local YugabyteDB cluster. Refer to [Set up your YugabyteDB cluster](https://docs.yugabyte.com/preview/explore/#set-up-your-yugabytedb-cluster) to start a local 3 node custer.

1. To start the application against a running local cluster, use the following command:

    ```sh
    java -jar ./yb-workload-sim-0.0.2.jar
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

### Run the application on a YugabyteDB Managed cluster

1. To connect the application to your cluster, ensure that you have downloaded the cluster SSL certificate and your computer is added to the IP allow list. Refer to [Before you begin](https://docs.yugabyte.com//preview/develop/build-apps/cloud-add-ip/).

1. To start the application against a running YugabyteDB Managed cluster, use the following command:

    ```sh
    java -Dnode=<host name> \
         -Ddbname=<dbname> \
         -Ddbuser=<dbuser> \
         -Ddbpassword=<dbpassword> \
         -Dssl=true \
         -Dsslmode=verify-full \
         -Dsslrootcert=<path-to-cluster-certificate> \
         -jar ./yb-workload-sim-0.0.2.jar
    ```

    Replace the following:

    * `host name` - the host name of your YugabyteDB cluster. For YugabyteDB Managed, select your cluster on the Clusters page, and click Settings. The host is displayed under Connection Parameters.

    * `dbname` - the name of the database you are connecting to (the default is yugabyte).

    * `dbuser` and `dbpassword` - the username and password for the YugabyteDB database. Use the credentials in the credentials file you downloaded when you created your cluster.

    * `path-to-cluster-certificate` with the path to the cluster certificate on your computer.

1. To view the application UI, navigate to `http://localhost:8080`.

    Additional parameters for YugabyteDB Managed to start/stop nodes and scale cluster can be provided in UI by clicking settings gear option on right hand side.

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

## How to build your own workload

1. Download the latest archive from the [releases](https://github.com/YugabyteDB-Samples/yb-workload-simulator/releases) page and unzip the file.

1. From the root of your project run the following maven command:

    ```sh
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```

    A jar file gets created at `<yb-workload-sim>/target/yb-simu-base-app.jar`

    You can also get the jar file in VS Code by right clicking the "WorkloadSimulationApplication.java" file and selecting Run Java.

    ![image](https://user-images.githubusercontent.com/78859174/196289685-74854a5a-1cb5-4b50-81b9-08534bab9a25.png)

    Verify the name of the jar file in the target directory, and use the name of that file wherever you see `yb-workload-sim-0.0.2.jar`.

    ![image](https://user-images.githubusercontent.com/78859174/196288218-13d499ee-a401-4b25-b95c-6e42c64a9824.png)

## Start a read and write workload

To view the application UI, navigate to <http://localhost:8080>

1. In the application UI, click the hamburger icon at the top of the page beside **Active Workloads for Generic**.

1. Select the **Usable Operations** tab.

1. Under **Create Tables**, click **Run Create Tables Workload** to add tables to the database.

1. Under **Seed Data**, click **Run Seed Data Workload** to add data to the tables.

1. Under **Simulation**, select the **Include new Inserts** option, and click **Run Simulation Workload**.

1. Click **Close**.

The Latency and Throughput charts show the workload running on the cluster.

## Create your own workload .java file

To create your workload file, do the following:

1. Copy the `GenericWorkload.java` file into a file with a new name. The file in this example is named "InstructionsWorkload.java".

    ![image](https://user-images.githubusercontent.com/78859174/196455986-2a4df344-26dc-4fbc-a153-e3b66a71cb6e.png)

    There are 3 "FIX ME" sections to change the name of the class after you copy and paste the "GenericWorkload.java" file.

    ![image](https://user-images.githubusercontent.com/78859174/196456157-fd832363-496b-4b25-ba38-1d9826013517.png)

    ![image](https://user-images.githubusercontent.com/78859174/196456219-19325cf8-5ac7-400f-830f-ff1ba155b8b5.png)

    ![image](https://user-images.githubusercontent.com/78859174/196456330-4d10f7cd-8931-4f19-b664-5a58b702cbe0.png)

1. To be able to call the new workload, you have to modify the `WorkloadConfig.java` file in [src/main/java/com/yugabyte/simulation/config/](src/main/java/com/yugabyte/simulation/config/).

    ![image](https://user-images.githubusercontent.com/78859174/196456659-71160757-e5f5-44fc-9b0a-c87897e27292.png)

    ![image](https://user-images.githubusercontent.com/78859174/196456775-84b931c4-fccf-4abc-aaeb-d3541ad8327d.png)

1. Run the "WorkloadSimulationApplication.java" as a java file.

    ![image](https://user-images.githubusercontent.com/78859174/196457069-5f47b875-51a2-48cc-ae48-cf8991cc93ea.png)

    ![image](https://user-images.githubusercontent.com/78859174/196457185-dfe2ee07-f1ba-4abf-9f59-718fdeb024df.png)

    A new "jar" file gets created in your target directory.

    ![image](https://user-images.githubusercontent.com/78859174/196457412-60f5adc1-aec1-4a14-b8ea-f63fd64a7acb.png)

1. You should be able to run the code with the following command (code from line 136 above):

    ![image](https://user-images.githubusercontent.com/78859174/196458425-88c0951f-c17e-41bb-98ec-d1fba7983128.png)

1. Navigate to <http://localhost:8080> and you will see the IP address of the machine you are using:

    ![image](https://user-images.githubusercontent.com/78859174/196458637-30acf3d6-7fed-49f1-a64e-f38447acc975.png)
