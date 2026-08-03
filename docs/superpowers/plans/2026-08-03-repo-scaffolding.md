# ManekPay Repo Scaffolding Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up the ManekPay monorepo skeleton — Maven multi-module backend, Docker Compose infra, nginx gateway, and a React frontend shell — so later phases (starting with the core ledger service) build on working scaffolding instead of blank folders.

**Architecture:** A Maven reactor at the repo root with five near-identical Spring Boot service modules under `services/`, each exposing only `/actuator/health`. Shared infra (Postgres, Redis, Kafka in KRaft mode) runs via Docker Compose; the five services and the frontend run locally during dev (`mvn spring-boot:run` / `npm run dev`) with an nginx container reverse-proxying to them on the host. No business logic, no DB schema, no auth, no CI — this plan only produces buildable, runnable, health-checkable scaffolding.

**Tech Stack:** Java 21, Spring Boot 3.3.5, Maven (multi-module), Vite + React 18 + TypeScript + Tailwind CSS, Docker Compose, nginx, PostgreSQL 16, Redis 7, Apache Kafka 3.8 (KRaft mode).

## Global Constraints

- Java 21 everywhere (spec: "Java 21 + Spring Boot 3.x").
- Every service module depends only on `spring-boot-starter-web` + `spring-boot-starter-actuator` — no controllers, entities, repositories, or DB/Kafka client libraries yet (spec: "No controllers, entities, repositories, or dependencies beyond ... at this stage").
- Ports are fixed: ledger-service 8081, fx-service 8082, vaults-service 8083, risk-service 8084, wealth-service 8085, nginx gateway 8080, frontend dev server 5173, postgres 5432, redis 6379, kafka 9092 (spec Ports table).
- One shared Postgres database/schema, not database-per-service (spec: explicit project decision).
- Kafka runs in KRaft mode — no Zookeeper container (spec: explicit decision).
- Gateway is nginx, not a Spring Cloud Gateway module (spec: explicit decision, avoid a 6th JVM service).
- No Flyway/DB schema, no Kafka topics/producers/consumers, no auth, no CI in this phase (spec: Out of Scope / Deferred).
- Root Maven coordinates: groupId `com.manekpay`, parent artifactId `manekpay-parent`, version `0.1.0-SNAPSHOT`.

---

### Task 1: Root Maven parent POM

**Files:**
- Create: `pom.xml` (repo root)

**Interfaces:**
- Consumes: nothing (first task).
- Produces: parent coordinates `groupId=com.manekpay`, `artifactId=manekpay-parent`, `version=0.1.0-SNAPSHOT` that every service module in Task 2 declares as its `<parent>`; Spring Boot BOM version `3.3.5` that all services inherit dependency versions from.

- [ ] **Step 1: Create the root parent POM**

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.manekpay</groupId>
  <artifactId>manekpay-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <spring-boot.version>3.3.5</spring-boot.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.springframework.boot</groupId>
          <artifactId>spring-boot-maven-plugin</artifactId>
          <version>${spring-boot.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>

  <modules>
    <module>services/ledger-service</module>
    <module>services/fx-service</module>
    <module>services/vaults-service</module>
    <module>services/risk-service</module>
    <module>services/wealth-service</module>
  </modules>
</project>
```

- [ ] **Step 2: Verify the POM is well-formed**

Run: `mvn -q validate`
Expected: fails with "Non-resolvable parent POM" or module-not-found style errors only (modules don't exist yet) — NOT an XML/schema error. This confirms the POM itself parses correctly before Task 2 adds the modules.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "Add root Maven parent POM for ManekPay services"
```

---

### Task 2: Five backend service skeletons

**Files:**
- Create: `services/ledger-service/pom.xml`
- Create: `services/ledger-service/src/main/java/com/manekpay/ledger/LedgerServiceApplication.java`
- Create: `services/ledger-service/src/main/resources/application.yml`
- Create: `services/ledger-service/Dockerfile`
- Create: `services/fx-service/pom.xml`
- Create: `services/fx-service/src/main/java/com/manekpay/fx/FxServiceApplication.java`
- Create: `services/fx-service/src/main/resources/application.yml`
- Create: `services/fx-service/Dockerfile`
- Create: `services/vaults-service/pom.xml`
- Create: `services/vaults-service/src/main/java/com/manekpay/vaults/VaultsServiceApplication.java`
- Create: `services/vaults-service/src/main/resources/application.yml`
- Create: `services/vaults-service/Dockerfile`
- Create: `services/risk-service/pom.xml`
- Create: `services/risk-service/src/main/java/com/manekpay/risk/RiskServiceApplication.java`
- Create: `services/risk-service/src/main/resources/application.yml`
- Create: `services/risk-service/Dockerfile`
- Create: `services/wealth-service/pom.xml`
- Create: `services/wealth-service/src/main/java/com/manekpay/wealth/WealthServiceApplication.java`
- Create: `services/wealth-service/src/main/resources/application.yml`
- Create: `services/wealth-service/Dockerfile`

**Interfaces:**
- Consumes: Task 1's parent coordinates (`com.manekpay:manekpay-parent:0.1.0-SNAPSHOT`, relative path `../../pom.xml`).
- Produces: five services, each responding `200 OK` on `GET /actuator/health` at its assigned port (ledger 8081, fx 8082, vaults 8083, risk 8084, wealth 8085) — this is what Task 5's nginx config proxies to.

All five services are structurally identical apart from name/package/port. Build them in this order: ledger-service, fx-service, vaults-service, risk-service, wealth-service.

- [ ] **Step 1: Create ledger-service**

Create `services/ledger-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.manekpay</groupId>
    <artifactId>manekpay-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>ledger-service</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Create `services/ledger-service/src/main/java/com/manekpay/ledger/LedgerServiceApplication.java`:

```java
package com.manekpay.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LedgerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerServiceApplication.class, args);
    }
}
```

Create `services/ledger-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8081

spring:
  application:
    name: ledger-service

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `services/ledger-service/Dockerfile`:

```dockerfile
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
```

- [ ] **Step 2: Build and verify ledger-service**

Run: `mvn -q -pl services/ledger-service -am package`
Expected: `BUILD SUCCESS`, producing `services/ledger-service/target/ledger-service-0.1.0-SNAPSHOT.jar`.

Run: `mvn -pl services/ledger-service spring-boot:run` (in background/separate terminal), then:
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/actuator/health`
Expected: `200`. Stop the service after confirming.

- [ ] **Step 3: Create fx-service**

Create `services/fx-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.manekpay</groupId>
    <artifactId>manekpay-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>fx-service</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Create `services/fx-service/src/main/java/com/manekpay/fx/FxServiceApplication.java`:

```java
package com.manekpay.fx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FxServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FxServiceApplication.class, args);
    }
}
```

Create `services/fx-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: fx-service

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `services/fx-service/Dockerfile`:

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY services/fx-service/pom.xml services/fx-service/pom.xml
RUN mvn -q -pl services/fx-service -DskipTests dependency:go-offline
COPY services/fx-service/src services/fx-service/src
RUN mvn -q -pl services/fx-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/services/fx-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 4: Build and verify fx-service**

Run: `mvn -q -pl services/fx-service -am package`
Expected: `BUILD SUCCESS`.

Run: `mvn -pl services/fx-service spring-boot:run` (background), then:
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/actuator/health`
Expected: `200`. Stop the service after confirming.

- [ ] **Step 5: Create vaults-service**

Create `services/vaults-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.manekpay</groupId>
    <artifactId>manekpay-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>vaults-service</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Create `services/vaults-service/src/main/java/com/manekpay/vaults/VaultsServiceApplication.java`:

```java
package com.manekpay.vaults;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class VaultsServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(VaultsServiceApplication.class, args);
    }
}
```

Create `services/vaults-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8083

spring:
  application:
    name: vaults-service

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `services/vaults-service/Dockerfile`:

```dockerfile
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
```

- [ ] **Step 6: Build and verify vaults-service**

Run: `mvn -q -pl services/vaults-service -am package`
Expected: `BUILD SUCCESS`.

Run: `mvn -pl services/vaults-service spring-boot:run` (background), then:
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8083/actuator/health`
Expected: `200`. Stop the service after confirming.

- [ ] **Step 7: Create risk-service**

Create `services/risk-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.manekpay</groupId>
    <artifactId>manekpay-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>risk-service</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Create `services/risk-service/src/main/java/com/manekpay/risk/RiskServiceApplication.java`:

```java
package com.manekpay.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RiskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskServiceApplication.class, args);
    }
}
```

Create `services/risk-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8084

spring:
  application:
    name: risk-service

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `services/risk-service/Dockerfile`:

```dockerfile
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
```

- [ ] **Step 8: Build and verify risk-service**

Run: `mvn -q -pl services/risk-service -am package`
Expected: `BUILD SUCCESS`.

Run: `mvn -pl services/risk-service spring-boot:run` (background), then:
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8084/actuator/health`
Expected: `200`. Stop the service after confirming.

- [ ] **Step 9: Create wealth-service**

Create `services/wealth-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.manekpay</groupId>
    <artifactId>manekpay-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>wealth-service</artifactId>
  <packaging>jar</packaging>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

Create `services/wealth-service/src/main/java/com/manekpay/wealth/WealthServiceApplication.java`:

```java
package com.manekpay.wealth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WealthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WealthServiceApplication.class, args);
    }
}
```

Create `services/wealth-service/src/main/resources/application.yml`:

```yaml
server:
  port: 8085

spring:
  application:
    name: wealth-service

management:
  endpoints:
    web:
      exposure:
        include: health
```

Create `services/wealth-service/Dockerfile`:

```dockerfile
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
```

- [ ] **Step 10: Build and verify wealth-service**

Run: `mvn -q -pl services/wealth-service -am package`
Expected: `BUILD SUCCESS`.

Run: `mvn -pl services/wealth-service spring-boot:run` (background), then:
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8085/actuator/health`
Expected: `200`. Stop the service after confirming.

- [ ] **Step 11: Build the whole reactor together**

Run: `mvn -q package`
Expected: `BUILD SUCCESS` for all 5 modules — confirms the root POM's `<modules>` list and every service's `<parent>`/`relativePath` are wired correctly together.

- [ ] **Step 12: Commit**

```bash
git add services/
git commit -m "Add five Spring Boot service skeletons (ledger, fx, vaults, risk, wealth)"
```

---

### Task 3: Docker Compose infra (Postgres, Redis, Kafka)

**Files:**
- Create: `docker-compose.yml`

**Interfaces:**
- Consumes: nothing.
- Produces: `postgres` reachable on `localhost:5432` (db/user/password all `manekpay`), `redis` on `localhost:6379`, `kafka` (KRaft, single broker) on `localhost:9092` — Task 5's nginx doesn't touch these directly, but they're the infra later ledger-service work will depend on.

- [ ] **Step 1: Create docker-compose.yml with postgres, redis, kafka**

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: manekpay
      POSTGRES_USER: manekpay
      POSTGRES_PASSWORD: manekpay
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  kafka:
    image: apache/kafka:3.8.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://:9092,CONTROLLER://:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@localhost:9093
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      CLUSTER_ID: ManekPayKRaftClusterId1

volumes:
  postgres-data:
```

- [ ] **Step 2: Verify the compose file is valid**

Run: `docker compose config --quiet`
Expected: no output, exit code `0`.

- [ ] **Step 3: Bring the infra up and verify each service**

Run: `docker compose up -d postgres redis kafka`
Expected: all three containers report `running` in `docker compose ps`.

Run: `docker compose exec postgres pg_isready -U manekpay`
Expected: `accepting connections`.

Run: `docker compose exec redis redis-cli ping`
Expected: `PONG`.

Run: `docker compose logs kafka --tail 50`
Expected: log lines showing the broker started (no repeated `ERROR`/crash-loop lines).

- [ ] **Step 4: Tear down**

Run: `docker compose down`
Expected: containers stop and are removed cleanly.

- [ ] **Step 5: Commit**

```bash
git add docker-compose.yml
git commit -m "Add Docker Compose infra: postgres, redis, kafka (KRaft)"
```

---

### Task 4: Frontend skeleton (Vite + React + TypeScript + Tailwind)

**Files:**
- Create: `frontend/` (scaffolded via Vite CLI)
- Modify: `frontend/src/main.tsx` (add Tailwind CSS import)
- Create: `frontend/tailwind.config.js`
- Create: `frontend/postcss.config.js`
- Modify: `frontend/src/index.css` (add Tailwind directives)

**Interfaces:**
- Consumes: nothing.
- Produces: a dev server on `localhost:5173` serving a blank page — this is what Task 5's nginx `/` route proxies to.

- [ ] **Step 1: Scaffold the Vite React-TS app**

Run (from repo root):
```bash
npm create vite@latest frontend -- --template react-ts
```

Expected: a `frontend/` directory is created with the standard Vite React-TS structure (`package.json`, `index.html`, `src/main.tsx`, `src/App.tsx`, etc.).

- [ ] **Step 2: Install dependencies and add Tailwind**

Run:
```bash
cd frontend
npm install
npm install -D tailwindcss postcss autoprefixer
```

- [ ] **Step 3: Configure Tailwind**

Create `frontend/tailwind.config.js`:

```javascript
/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {},
  },
  plugins: [],
};
```

Create `frontend/postcss.config.js`:

```javascript
export default {
  plugins: {
    tailwindcss: {},
    autoprefixer: {},
  },
};
```

Replace the contents of `frontend/src/index.css` with:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

- [ ] **Step 4: Verify the dev server runs**

Run: `npm run dev -- --port 5173` (from `frontend/`, background/separate terminal)

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:5173/`
Expected: `200`.

Stop the dev server after confirming.

- [ ] **Step 5: Commit**

```bash
cd ..
git add frontend/
git commit -m "Add Vite + React + TypeScript + Tailwind frontend skeleton"
```

---

### Task 5: Nginx gateway + end-to-end wiring

**Files:**
- Create: `gateway/nginx.conf`
- Modify: `docker-compose.yml` (add `nginx` service)

**Interfaces:**
- Consumes: Task 2's five service ports (8081–8085), Task 4's frontend port (5173).
- Produces: a gateway on `localhost:8080` — `/` proxies to the frontend, `/api/<service>/*` proxies to the matching backend. This is the final integration point; nothing later in this plan depends on it.

- [ ] **Step 1: Create the nginx gateway config**

Create `gateway/nginx.conf`:

```nginx
events {}

http {
    server {
        listen 80;

        location / {
            proxy_pass http://host.docker.internal:5173;
            proxy_set_header Host $host;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
        }

        location /api/ledger/ {
            proxy_pass http://host.docker.internal:8081/;
        }

        location /api/fx/ {
            proxy_pass http://host.docker.internal:8082/;
        }

        location /api/vaults/ {
            proxy_pass http://host.docker.internal:8083/;
        }

        location /api/risk/ {
            proxy_pass http://host.docker.internal:8084/;
        }

        location /api/wealth/ {
            proxy_pass http://host.docker.internal:8085/;
        }
    }
}
```

- [ ] **Step 2: Add the nginx service to docker-compose.yml**

Modify `docker-compose.yml`, adding this service alongside `postgres`, `redis`, and `kafka` (under the top-level `services:` key):

```yaml
  nginx:
    image: nginx:1.27-alpine
    ports:
      - "8080:80"
    volumes:
      - ./gateway/nginx.conf:/etc/nginx/nginx.conf:ro
    extra_hosts:
      - "host.docker.internal:host-gateway"
```

- [ ] **Step 3: Verify the compose file is still valid**

Run: `docker compose config --quiet`
Expected: no output, exit code `0`.

- [ ] **Step 4: End-to-end verification**

Start everything:
```bash
docker compose up -d nginx
mvn -pl services/ledger-service spring-boot:run &
```
(from `frontend/`) `npm run dev -- --port 5173 &`

Wait for both to report ready, then:

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/`
Expected: `200` (nginx → frontend dev server).

Run: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/ledger/actuator/health`
Expected: `200` (nginx → ledger-service `/actuator/health`).

Stop the backend, frontend, and `docker compose down` after confirming both.

- [ ] **Step 5: Commit**

```bash
git add gateway/ docker-compose.yml
git commit -m "Add nginx gateway and wire routing to frontend and backend services"
```
