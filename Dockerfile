# Multi-stage Dockerfile for EWB Standing Order Platform
# Stage 1: Build all service artifacts
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Copy parent and module definitions
COPY pom.xml .
COPY common/pom.xml common/
COPY config-server/pom.xml config-server/
COPY eureka-server/pom.xml eureka-server/
COPY gateway-service/pom.xml gateway-service/
COPY standing-order-service/pom.xml standing-order-service/
COPY execution-service/pom.xml execution-service/
COPY payment-service/pom.xml payment-service/
COPY notification-service/pom.xml notification-service/

# Download dependencies offline where possible
RUN mvn dependency:go-offline -B || true

# Copy all source trees and configurations
COPY common common
COPY config-repo config-repo
COPY config-server config-server
COPY eureka-server eureka-server
COPY gateway-service gateway-service
COPY standing-order-service standing-order-service
COPY execution-service execution-service
COPY payment-service payment-service
COPY notification-service notification-service

# Build and package all services
RUN mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ARG SERVICE_NAME
COPY --from=builder /workspace/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080 8081 8082 8083 8084 8761 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
