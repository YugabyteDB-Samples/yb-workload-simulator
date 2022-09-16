#!/bin/sh

java -Djava.security.egd=file:/dev/./urandom  -Dspring.profiles.active="" -Dspring.datasource.hikari.maximumPoolSize=20 -Dspring.workload="simpleSelectWorkload" -jar ../../../../target/yb-workload-simu-app.jar
