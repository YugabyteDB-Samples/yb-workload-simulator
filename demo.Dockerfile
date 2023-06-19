ARG JAVA_VERSION=19

ARG JDK_IMAGE=eclipse-temurin
ARG JDK_IMAGE_TAG=${JAVA_VERSION}-jdk

ARG JRE_IMAGE=eclipse-temurin
ARG JRE_IMAGE_TAG=${JAVA_VERSION}-jre
ARG JAR_FILE=yb-workload-sim.jar
ARG APP_NAME=workload-simulator
ARG APP_PORT=8080

FROM ${JDK_IMAGE}:${JDK_IMAGE_TAG} as build
RUN mkdir -p /opt/workspace
WORKDIR /opt/workspace
COPY pom.xml ./mvnw ./
COPY .mvn/ .mvn/
RUN ./mvnw -B clean -DskipDockerBuild -DskipTests dependency:resolve-plugins dependency:resolve
ADD . .
RUN ./mvnw -B clean package -DskipTests


FROM ${JRE_IMAGE}:${JRE_IMAGE_TAG} as main
ARG JAR_FILE
LABEL MAINTAINER YugaByte
ENV container=${APP_NAME}

WORKDIR /opt/yugabyte
COPY --from=build /opt/workspace/target/${JAR_FILE} /opt/yugabyte/
USER nobody

EXPOSE ${APP_PORT}
ENV JAR_FILE=${JAR_FILE}
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS}  -jar /opt/yugabyte/${JAR_FILE}"]
