## What is yb-simulation-base-demo-app
This is our base app containing some generic workloads. You can create/drop tables for a workload, load data, run different simulations all from UI. Web page provides you with live latency and throughput about your yugabyte db cluster while these simulations are running. This repo acts as a base and you can extend it to write your specific simulations. I will be providing some starter projects/instructions for that soon. 

## Prerequisites to run locally
Here are the steps to get the demo application to run on your local machine, starting with Yugabyte.
Have Yugabyte installed on your local machine: https://docs.yugabyte.com/preview/quick-start/.
Open a total of 3 IP addresses to be able to run Yugabyte with a replication factor of 3
Use this command:
```
sudo ifconfig lo0 alias 127.0.0.2
sudo ifconfig lo0 alias 127.0.0.3
```
Go into the Yugabyte directory to start a 3 node cluster locally with the following command 
```
./bin/yb-ctl --rf 3 create. 
```
To verify that Yugabyte is up and running you can run the command
```
./bin/yb-ctl status
```
Please have Java installed on your local machine. Here is a link to help with that process. 

https://www.youtube.com/watch?v=FsX0_RXMwvY


![image](https://user-images.githubusercontent.com/78859174/192043252-88b9578d-7e79-49cd-bae6-486c8df1bf0c.png)

To verify you have Java installed please run this command:
```
java -version
```
For this demo we also need to install Maven, here is a link I have found helpful to install Maven on a Mac M1 computer.
https://www.youtube.com/watch?v=kCQKh_CscYA

To confirm you have successfully install Maven on your Mac please use the command
```
mvn -v
```
![image](https://user-images.githubusercontent.com/78859174/192044014-ff113c98-24db-4b5a-aa0e-e12373f72cf6.png)


## Code setup and Installation
Download the zip file

### How to build the App
Go to the root of the project and run:
```
/mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
This will create the jar file: <yb-simulation-base-demo-app>/target/yb-simulation-base-app.jar

Now you can go to http://localhost:8080 and you will see this

![image](https://user-images.githubusercontent.com/78859174/192045736-8cbb6ae0-9bd5-4fc6-ae0d-c2830192b669.png)

From here you can select the “hamburger” menu at the top and simulate your own workload!

Now go back to your terminal and within the Yugabyte folder (the same place where you started the node cluster). And you can stop a node by executing the following command 
```
./bin/yb-ctl stop_node 2
```
![image](https://user-images.githubusercontent.com/78859174/192045896-003b5a72-d97c-4521-96a5-0f0e8cb63c93.png)

As you can see there is a slight drop when the node is stopped and then it just keeps going!
  
![image](https://user-images.githubusercontent.com/78859174/192046016-54f475fb-9237-41bc-a85a-5ea8036409ea.png)
Note: The Stop node button is still in Beta at the time of this documentation. 
  

## YBA Demo
### How to run this App
This is a walk through of how to get the Demo Up in a YBA (Yugabyte Anywhere UI) environment in AWS. Here is a link to the repo: https://github.com/yugabyte/yb-simulation-base-demo-app

The first thing that is needed is for you to complete the steps above in order to create the “jar” file. After that there will be a “yb-simu-base-app.jar” file in the “target” directory of the “yb-simulation-base-demo-app-main” folder 

Lets go over some prerequisites: You need everything above including Java, Maven, and Yugabyte installed on your local machine. 

You should have Yugabyte “stopped” on your local machine. If you followed the steps about you should restart node 2 by typing 
```
./bin/yb-ctl start_node 2
```
![image](https://user-images.githubusercontent.com/78859174/192046522-6ca91219-e137-456f-b6cf-ba800fc11a61.png)

  
Next we need to stop the cluster: 
```
./bin/yb-ctl stop
```
![image](https://user-images.githubusercontent.com/78859174/192046645-8d7926a8-4304-4ff2-9b49-605400d3ae45.png)
 
To successfully follow along with this demo please make sure you have an YBA set up in AWS. If you need help with that please follow these instructions: 
https://docs.google.com/presentation/d/1LbmaLWURFNc4f4pv2KUSntE8eDf6ohbk/edit#slide=id.p13

You should have an environment that looks like this: IP addresses excluded for security.
![image](https://user-images.githubusercontent.com/78859174/192046695-1c4ff20a-b4dd-4cbf-bacd-db432680b706.png)

Now we can get started with running the simulation demo

The first thing we need to do is to move the “jar” file from our local directory onto the ec2 instance. You can do this from the directory the jar file is located in on your local machine or move it to the current working directory. Then we can move it with the following command: 
```
scp -i <path to your pem file> <name of the jar file> ec2-user@<IP address of the YBA>:/tmp/
```
![image](https://user-images.githubusercontent.com/78859174/192046830-32107334-f447-4521-8c1f-3f1b1492e8cf.png)

Please note the above is an example and replace all the parameters of what you need to access your YBA instance with your own credentials. Pem location, file name, ip address.

There are two ways to launch the application from inside your EC2 instance. You can launch it with the following command inside the terminal:
  
First install java on the machine: 
```
sudo yum install java
```
Navigate to your tmp directory and use ```mkdir logs``` to make a log file in case there are any errors during the set up. 
Then execute the commands below in the terminal or navigate to your tmp directory and secret an executable script. Note the Dnode IP address should be that of one in your AWS cluster

 
EXAMPLE  
  
![image](https://user-images.githubusercontent.com/78859174/192047573-ce34a4ca-7e0d-4918-b8dd-14fec9b309a3.png)
  
 Code
 ```
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
```  
java -DXmx=16g -Dmax-pool-size=10 -Dnode=<your node IP> -Ddbuser=yugabyte -Ddbpassword=<Your password> -Dspring.datasource.hikari.data-source-properties.topologyKeys=<Your AWS credentials> Example:aws.us-east-1.us-east-1a,aws.us-east-1.us-east-1b,aws.us-east-1.us-east-1c -Dspring.workload=genericWorkload -DadditionalEndpoints= -jar yb-simu-base-app.jar
```
  
The second way to do this is to make an executable script with the command above in case you wish to rerun the command. To do that you can copy and paste the code into a .sh file here is an example: ```touch run.sh``` then make the file executable by typing ```chmod 700 run.sh.``` Then you can execute it by typing ```./run.sh```

Now we can navigate to the IP of the YBA cluster(not the IP of the node)with the port 8080.
![image](https://user-images.githubusercontent.com/78859174/192047682-7bfaba93-9164-49e1-a205-2eb5bab2a50f.png)


  
  
  
  
  
  ```
java -DXmx=16g -Dmax-pool-size=10 -Dnode=<database-ip-or-name> -Ddbuser=<db-user-id> -Ddbpassword=<db-password> -Dspring.datasource.hikari.data-source-properties.topologyKeys=<cloud.region.zone> -Dspring.workload=genericWorkload -jar yb-simu-base-app.jar
```
  
To try other work loads you can replace the genericWorkload from -Dworkload= line with any other workload from this section src>>main>java>com>yugabyte>simulation>service https://github.com/yugabyte/workload-simulation-demo-app/tree/main/src/main/java/com/yugabyte/simulation/service

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
![Screenshot](docs/image1.png)
![Screenshot](docs/image2.png)
