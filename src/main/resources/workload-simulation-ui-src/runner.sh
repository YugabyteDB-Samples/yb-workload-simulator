#!/bin/bash

#/usr/bin/java -Dspring.datasource.hikari.data-source-properties.serverName=172.151.57.146 -Dspring.datasource.hikari.maximumPoolSize=40 -Dspring.workload="NewFormatWorkload" -DworkloadType="RUN_SIMULATION" -Dparams="1000,0,1" -jar target/yb-workload-simu-app.jar | tee output.txt
/usr/bin/java -Dspring.datasource.hikari.data-source-properties.serverName=172.151.45.137 -Dspring.datasource.hikari.maximumPoolSize=100 -Dspring.workload="NewFormatWorkload" -Dpassword="cacDemo1!" -jar yb-workload-simu-app.jar


