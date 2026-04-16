FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy the BOM and install it to the local maven repo
COPY patient-management-bom ./patient-management-bom
RUN mvn install -f patient-management-bom/pom.xml -B

# Copy and install the aggregator (parent) POM
COPY pom.xml ./pom.xml
RUN mvn install -N -B

# Copy the service project
COPY appointment-service ./appointment-service

# Build the service from its own directory
WORKDIR /app/appointment-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk AS runner

WORKDIR /app

COPY --from=builder /app/appointment-service/target/appointment-service-1.0-SNAPSHOT.jar ./app.jar

EXPOSE 8084

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-XX:InitialRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-jar", "app.jar"]
