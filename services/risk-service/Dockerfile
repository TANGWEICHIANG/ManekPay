FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/risk-service/pom.xml services/risk-service/pom.xml
RUN mvn -q -pl services/risk-service -DskipTests dependency:go-offline
COPY services/risk-service/src services/risk-service/src
RUN mvn -q -pl services/risk-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/risk-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
