FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Install required packages for build
RUN apk add --no-cache maven

# Copy pom.xml first for better caching of dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src/ /app/src/

# Build the application
RUN mvn package -DskipTests

# Create runtime image - using JDK instead of JRE to have javac available
FROM eclipse-temurin:17-jdk-alpine

# Install required tools for code execution
RUN apk add --no-cache \
    python3 \
    nodejs \
    npm \
    g++ \
    bash

# Verify Java compiler is available
RUN javac -version

# Install TypeScript tools
RUN npm install -g typescript ts-node

# Create a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory for application
WORKDIR /home/appuser/app

# Copy the jar from the build stage
COPY --from=build --chown=appuser:appgroup /app/target/*.jar /home/appuser/app/app.jar

# Create a temp directory for code execution with proper permissions
RUN mkdir -p /home/appuser/app/tmp && \
    chmod 777 /home/appuser/app/tmp

# Ensure Java executables are available to non-root user
RUN mkdir -p /home/appuser/bin && \
    ln -s $(which javac) /home/appuser/bin/javac && \
    ln -s $(which java) /home/appuser/bin/java && \
    chown -R appuser:appgroup /home/appuser/bin

# Add to PATH environment
ENV PATH="/home/appuser/bin:${PATH}"

# Set temp directory environment variable
ENV TEMP_DIR=/home/appuser/app/tmp

# Switch to non-root user
USER appuser

# Expose the port the app runs on
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "/home/appuser/app/app.jar"] 