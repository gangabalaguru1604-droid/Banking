
# Build stage
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.war app.war

# Render web service port
# Run Spring Boot app with dynamic port and optimized JVM flags for Render 512MB RAM limits
CMD ["sh", "-c", "java ${JAVA_OPTS:--Xms256m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ExitOnOutOfMemoryError} -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar app.war"]
