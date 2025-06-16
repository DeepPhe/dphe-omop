# Use OpenJDK 8 as base image
FROM openjdk:11-jre-bullseye

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -g 1001 appgroup && \
    useradd -u 1001 -g appgroup -m appuser

# Create directory structure matching your app's expectations
RUN mkdir -p /app/data/input \
             /app/data/output \
             /app/resources/dphe-db-resources/neo4j \
             /app/resources/dphe-db-resources/hsqldb \
             /app/resources/pipeline \
             /app/logs

# Copy the fat jar
COPY target/deepphe-omop-0.1.0.jar /app/deepphe-omop-0.1.0.jar

# Copy database files and pipeline resources
COPY src/main/resources/dphe-db-resources/ /app/resources/dphe-db-resources/
COPY src/main/resources/pipeline/ /app/resources/pipeline/

# Create Neo4j logs directory and ensure proper permissions
RUN mkdir -p /app/resources/dphe-db-resources/neo4j/logs && \
    chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port if your app has a web interface
EXPOSE 8080

# Health check - adjust this based on your app's behavior
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD pgrep -f "deepphe-omop" || exit 1

# Default command - can be overridden
ENTRYPOINT ["java", \
    "-cp", "deepphe-omop-0.1.0.jar", \
    "org.healthnlp.deepphe.omop.DpheOmopDocRunner", \
    "-i", "/app/data/input", \
    "-o", "/app/data/output", \
    "-p", "/app/resources/pipeline/OmopDocRunner.piper"]
