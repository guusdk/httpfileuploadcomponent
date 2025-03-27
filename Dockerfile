FROM maven:3-eclipse-temurin-17
COPY . .
RUN mvn clean package

FROM eclipse-temurin:17-alpine
COPY --from=0 /target/httpfileuploadcomponent-*-SNAPSHOT-jar-with-dependencies.jar /opt/httpfileuploadcomponent.jar
ENTRYPOINT ["java", "-jar", "/opt/httpfileuploadcomponent.jar"]
