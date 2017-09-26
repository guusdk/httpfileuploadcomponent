FROM maven:3-jdk-8
COPY . .
RUN mvn clean package

FROM openjdk:8-jre-alpine
COPY --from=0 /target/httpfileuploadcomponent-*-SNAPSHOT-jar-with-dependencies.jar /opt/httpfileuploadcomponent.jar
ENTRYPOINT ["java", "-jar", "/opt/httpfileuploadcomponent.jar"]
