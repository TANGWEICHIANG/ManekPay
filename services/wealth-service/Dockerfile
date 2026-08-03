FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/wealth-service/pom.xml services/wealth-service/pom.xml
RUN mvn -q -pl services/wealth-service -DskipTests dependency:go-offline
COPY services/wealth-service/src services/wealth-service/src
RUN mvn -q -pl services/wealth-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/wealth-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
