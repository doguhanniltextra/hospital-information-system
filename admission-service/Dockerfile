FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy the BOM and install it to the local maven repo
COPY patient-management-bom ./patient-management-bom
RUN mvn install -f patient-management-bom/pom.xml -B

# Copy and install the aggregator (parent) POM
COPY pom.xml ./pom.xml
RUN mvn install -N -B

# Copy the service project
COPY admission-service ./admission-service

# Build the service from its own directory
WORKDIR /app/admission-service
RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:21-jre-alpine AS runner

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/admission-service/target/admission-service-0.0.1-SNAPSHOT.jar ./app.jar

USER appuser

EXPOSE 8086

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
