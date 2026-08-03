FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/vaults-service/pom.xml services/vaults-service/pom.xml
RUN mvn -q -pl services/vaults-service -DskipTests dependency:go-offline
COPY services/vaults-service/src services/vaults-service/src
RUN mvn -q -pl services/vaults-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/vaults-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
