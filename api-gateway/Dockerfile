FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY . .
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/target/api-gateway-1.0-SNAPSHOT.jar ./app.jar

USER appuser

EXPOSE 4004

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
