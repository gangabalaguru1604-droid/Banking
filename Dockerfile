# Multi-stage Docker build for Indian Bank Loan App from repository root
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven settings mirror and POM first to leverage Docker layer caching
COPY loan_project/loan_app/settings.xml /root/.m2/settings.xml
COPY loan_project/loan_app/settings.xml .
COPY loan_project/loan_app/pom.xml .

# Pre-fetch dependencies using high-speed mirror (bypasses Maven Central 429 rate limiting on Render)
RUN mvn dependency:go-offline -s settings.xml -B || true

# Copy application source code
COPY loan_project/loan_app/src ./src

# Build production package without running tests during image build
RUN mvn clean package -DskipTests -s settings.xml -B

# Lightweight JRE runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy built war from build stage
COPY --from=build /app/target/*.war app.war

# Default cloud port exposed
EXPOSE 8080

# Run Spring Boot app with dynamic port assigned by Render/Cloud provider and optimized JVM memory for Render 512MB RAM limit
CMD ["sh", "-c", "java ${JAVA_OPTS:--Xms128m -Xmx256m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError} -Dserver.port=${PORT:-8080} -jar app.war"]
