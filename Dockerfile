# Production Runtime Docker image for Spring Boot Recommendation Engine
FROM eclipse-temurin:21-jre

WORKDIR /app

# Install curl for container health checks
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Create non-root user safely
RUN (id -u appuser 2>/dev/null || useradd -m -s /bin/bash appuser)

# Copy built jar and dataset into container
COPY target/*.jar app.jar
COPY consolidated_data.csv ./consolidated_data.csv

# Change ownership to non-root user
RUN chown -R appuser:appuser /app

USER appuser

# Expose application port
EXPOSE 8080

# Health check against Spring Boot Actuator endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]