FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Cache Maven dependencies in a dedicated layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build production jar
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/auth-service-0.0.1-SNAPSHOT.jar ./app.jar
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8089

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]

