YB Workload Simulator application

YB Workload Simulator is a Java application that simulates workloads against YugabyteDB and provides live metrics of latency and throughput from the application's point of view. You can create/drop tables for a workload, load data, run different simulations from the application UI. You can view latency and throughput metrics in real time for your YugabyteDB cluster when the simulations are running. This repository acts as a base and you can extend it to write your specific simulations.

<!-- We will be providing some starter projects/instructions for that soon. -->

## Download the jar

YB Workload Simulator requires Java 11 or later installed on your computer. JDK installers for Linux and macOS can be downloaded from [Oracle](http://jdk.java.net/), [Adoptium (OpenJDK)](https://adoptium.net/), or [Azul Systems (OpenJDK)](https://www.azul.com/downloads/?package=jdk). Homebrew users on macOS can install using `brew install openjdk`.

Download the YB Workload Simulator JAR file (yb-simu-base-app.jar) using the following command:

```sh
wget https://github.com/YugabyteDB-Samples/yb-workload-simulator/releases/download/1.0/yb-simu-base-app.jar
```

## Run the application locally

Install a local YugabyteDB cluster. Refer to [Set up your YugabyteDB cluster](https://docs.yugabyte.com/preview/explore/#set-up-your-yugabytedb-cluster) to start a local 3 node custer.

To start the application against a running local cluster, use the following command:

```sh
java -jar ./yb-simu-base-app.jar
```

By default, the application connects to a local YugabyteDB cluster at 127.0.0.1.

To connect to a different address or node, use the `-Dnode` flag to specify an IP address. For example:

```sh
java -Dnode=127.0.0.2 -jar ./yb-simu-base-app.jar
```

To view the application UI, navigate to `http://localhost:8080`.

You can pass additional parameters as needed:

```sh
java -DXmx=16g
     -Dmax-pool-size=10
     -Dnode=<database-ip-or-name>
     -Ddbuser=<db-user-id>
     -Ddbpassword=<db-password>
     -Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone>
     -Dspring.workload=genericWorkload
     -jar yb-simu-base-app.jar
```

Parameters you can add to above java command are as follows:

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

## Run the application on a YugabyteDB Managed cluster

To connect the application to your cluster, ensure that you have downloaded the cluster SSL certificate and your computer is added to the IP allow list. Refer to [Before you begin](https://docs.yugabyte.com//preview/develop/build-apps/cloud-add-ip/).

To start the application against a running YugabyteDB Managed cluster, use the following command:

```sh
java -Dnode=<host name> \
     -Ddbname=<dbname> \
     -Ddbuser=<dbuser> \
     -Ddbpassword=<dbpassword> \
     -Dssl=true \
     -Dsslmode=verify-full \
     -Dsslrootcert=<path-to-cluster-certificate> \
     -jar ./yb-simu-base-app.jar
```

Replace the following:

- `host name` - the host name of your YugabyteDB cluster. For YugabyteDB Managed, select your cluster on the Clusters page, and click Settings. The host is displayed under Connection Parameters.

- `dbname` - the name of the database you are connecting to (the default is yugabyte).

- `dbuser` and `dbpassword` - the username and password for the YugabyteDB database. Use the credentials in the credentials file you downloaded when you created your cluster.

- `path-to-cluster-certificate` with the path to the cluster certificate on your computer.

To view the application UI, navigate to `http://localhost:8080`.

Additional parameters for YugabyteDB Managed to start/stop nodes and scale cluster from simulation application UI are as follows:

```sh
-Dybm-account-id=<YBM Account Id>
-Dybm-api-key=<YBM API Key>
-Dybm-project-id=<YBM Project Id>
-Dybm-cluster-id=<YBM Cluster Id>
```

## Code setup and Installation

### Build the jar file

From the root of the project run the following maven command:

```sh
mvn clean package -DskipTests
```

A jar file gets created at : <yb-workload-simulator>/target/yb-workload-simulator.jar. You can [run the application locally](https://github.com/YugabyteDB-Samples/yb-workload-simulator/edit/main/README.md#run-the-application-locally) or using [YugabyteDB Managed](https://github.com/YugabyteDB-Samples/yb-workload-simulator/edit/main/README.md#run-the-application-on-a-yugabytedb-managed-cluster) using the jar.

#### Additional parameters if you wish to run YCQL workload

```sh
-Dworkload=genericCassandraWorkload
-Dspring.data.cassandra.contact-points=<host ip>
-Dspring.data.cassandra.port=9042
-Dspring.data.cassandra.local-datacenter=<datacenter> [ex. us-east-2 ]
-Dspring.data.cassandra.userid=cassandra
-Dspring.data.cassandra.password=<cassandra-password>
```

## How to build your own workload

1. Download the [workload simulator zip](https://github.com/YugabyteDB-Samples/yb-workload-simulator/archive/refs/heads/main.zip) file.

1. From the root of the project run the following maven command:

    ```sh
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
    ```

    A jar file gets created at `<yb-workload-simulator>/target/yb-workload-simulator.jar`

    You can also get the jar file in VS Code by right clicking the "WorkloadSimulationApplication.java" file and selecting Run Java.

    ![image](https://user-images.githubusercontent.com/78859174/196289685-74854a5a-1cb5-4b50-81b9-08534bab9a25.png)

    You can verify the name of the "jar" file from the target directory using the name of that file where ever you see "yb-workload-simulation.jar"

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

<!-- ## How to setup YBA Environment

### How to run this App

This is a walk through of how to get the Demo up in a YBA (YugabyteDB Anywhere UI) environment in AWS.

The first thing that is needed is for you to complete the steps above in order to create the JAR file. After that there will be a `yb-simu-base-app.jar` file in the “target” directory of the “yb-simulation-base-demo-app-main” folder

Lets go over some prerequisites: You need everything above including Java, Maven, and YugabyteDB installed on your local machine. It should be noted that if this is your first time using the AWS pem file you need to change the permistions of the .pem file with the following command:

```sh
chmod 400 <name of your .pem file>
```

![image](https://user-images.githubusercontent.com/78859174/192046522-6ca91219-e137-456f-b6cf-ba800fc11a61.png)

![image](https://user-images.githubusercontent.com/78859174/192046645-8d7926a8-4304-4ff2-9b49-605400d3ae45.png)

To successfully follow along with this demo please make sure you have an YBA set up in AWS. If you need help with that please follow these instructions:
https://docs.google.com/presentation/d/1LbmaLWURFNc4f4pv2KUSntE8eDf6ohbk/edit#slide=id.p13

You should have an environment that looks like this: IP addresses excluded for security.
![image](https://user-images.githubusercontent.com/78859174/192046695-1c4ff20a-b4dd-4cbf-bacd-db432680b706.png)

Now we can get started with running the simulation demo

The first thing we need to do is to move the “jar” file from our local directory onto the ec2 instance. You can do this from the directory the jar file is located in on your local machine or move it to the current working directory. Then we can move it with the following command:

```sh
scp -i <path to your pem file> <name of the jar file> ec2-user@<IP address of the YBA>:/tmp/
```

![image](https://user-images.githubusercontent.com/78859174/192046830-32107334-f447-4521-8c1f-3f1b1492e8cf.png)

Please note the above is an example and replace all the parameters of what you need to access your YBA instance with your own credentials. Pem location, file name, ip address.

There are two ways to launch the application from inside your EC2 instance. You can launch it with the following command inside the terminal:

First install java on the machine:

```sh
sudo yum install java
```

Navigate to your tmp directory and use `mkdir logs` to make a log file in case there are any errors during the set up. Then execute the commands below in the terminal or navigate to your tmp directory and secret an executable script. Note the Dnode IP address should be that of one in your AWS cluster.

EXAMPLE

![image](https://user-images.githubusercontent.com/78859174/192047573-ce34a4ca-7e0d-4918-b8dd-14fec9b309a3.png)

Code

```sh
java -DXmx=32g -Dspring.datasource.hikari.maximumPoolSize=100 -DloggingDir="/tmp/logs"\
-Dnode=<Your Node IP address>\
-Ddbuser=<Your username> Default:yugabyte \
-Ddbpassword=<Your password> Default:yugabyte \
-Dport=5433 \
-Dmax-pool-size=100 \
-Ddbname=yugabyte \
-Dspring.profiles.active=application.yaml \
-Dserver.port=8080 \
-DidCounter=1 \
-Dssl=false <unless you enabled it> \
-Dsslmode=disable \
-Dworkload=genericWorkload \
-DadditionalEndpoints= \
-jar yb-workload-simu-app.jar
 ```

You can also use this code to specify the AWS regions:

```sh
java -DXmx=16g -Dmax-pool-size=10 -Dnode=<your node IP> -Ddbuser=yugabyte -Ddbpassword=<Your password> -Dspring.datasource.hikari.data-source-properties.topologyKeys=<Your AWS regions/zones> Example:aws.us-east-1.us-east-1a,aws.us-east-1.us-east-1b,aws.us-east-1.us-east-1c -Dspring.workload=genericWorkload -DadditionalEndpoints= -jar yb-simu-base-app.jar
```

The second way to do this is to make an executable script with the command above in case you wish to rerun the command. To do that you can copy and paste the code into a .sh file here is an example: `touch run.sh` then make the file executable by typing `chmod 700 run.sh` Then you can execute it by typing `./run.sh`

Now we can navigate to the IP of the YBA cluster(not the IP of the node)with the port 8080.

![image](https://user-images.githubusercontent.com/78859174/192047682-7bfaba93-9164-49e1-a205-2eb5bab2a50f.png)

```sh
java -DXmx=16g -Dmax-pool-size=10 -Dnode=<database-ip-or-name> -Ddbuser=<db-user-id> -Ddbpassword=<db-password> -Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone> -Dspring.workload=genericWorkload -jar yb-simu-base-app.jar
```

To try other work loads you can replace the genericWorkload from -Dworkload= line with any other workload from this section src>>main>java>com>yugabyte>simulation>service https://github.com/yugabyte/workload-simulation-demo-app/tree/main/src/main/java/com/yugabyte/simulation/service

#### Additional parameters if you wish to run YCQL workload

(Please remember to move the `-jar yb-simu-base-app.jar` to the last line of the script. Everything after that line gets ignored.)

```sh
-Dworkload=genericCassandraWorkload
-Dspring.data.cassandra.contact-points=<host ip>
-Dspring.data.cassandra.port=9042
-Dspring.data.cassandra.local-datacenter=<datacenter> [ex. us-east-2 ]
-Dspring.data.cassandra.userid=cassandra
-Dspring.data.cassandra.password=<cassandra-password>
```

#### Local Environment

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
``` -->

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
