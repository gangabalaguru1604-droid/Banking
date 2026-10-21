# Multi-stage Docker build for Indian Bank Loan App from repository root
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven project descriptor and source
COPY loan_project/loan_app/pom.xml .
COPY loan_project/loan_app/src ./src

# Build package without running tests during image build
RUN mvn clean package -DskipTests

# Lightweight JRE runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy built war from build stage
COPY --from=build /app/target/*.war app.war

# Default cloud port exposed
EXPOSE 8080

# Run Spring Boot app with dynamic port assigned by Render/Cloud provider and optimized JVM memory for Render 512MB RAM limit
CMD ["sh", "-c", "java ${JAVA_OPTS:--Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError} -Dserver.port=${PORT:-8080} -jar app.war"]
