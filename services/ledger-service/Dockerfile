FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/ledger-service/pom.xml services/ledger-service/pom.xml
RUN mvn -q -pl services/ledger-service -DskipTests dependency:go-offline
COPY services/ledger-service/src services/ledger-service/src
RUN mvn -q -pl services/ledger-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/ledger-service/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
