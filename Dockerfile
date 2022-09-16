FROM openjdk:8-jre-alpine
MAINTAINER YugaByte
ENV container=yb-workload-simu-app

WORKDIR /opt/yugabyte

ARG JAR_FILE
ADD target/${JAR_FILE} /opt/yugabyte/yb-workload-simu-app.jar

#ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/yugabyte/yb-workload-simu-app.jar"]
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /opt/yugabyte/yb-workload-simu-app.jar"]