# DeepPhe OMOP

A Dockerized application for processing clinical documents using the DeepPhe natural language processing pipeline at the doc-level (not patient-level).
Although the output does not strictly conform to the OMOP NoteNLP schema, it provides the output of DeepPhe in a tabular
format that can be easily integrated into the OMOP ETL process by selecting the appropriate fields.

## Overview

DeepPhe OMOP is a clinical text processing application that:
- Processes clinical documents using the DeepPhe NLP pipeline
- Extracts cancer-related information from unstructured text
- Converts the extracted data to OMOP CDM format
- Runs in a containerized environment for easy deployment and scalability

## Prerequisites

- Docker and Docker Compose installed
- Java 11 (for building the JAR file)
- Maven (for building the project)

## Project Structure

```
deepphe-omop/
├── build-and-deploy.sh          # Main deployment script
├── docker-compose.yml           # Docker Compose configuration
├── Dockerfile                   # Container definition
├── target/
│   └── deepphe-omop-0.1.0.jar   # Application JAR (built by Maven)
├── src/main/resources/
│   ├── dphe-db-resources/       # Database resources
│   │   ├── neo4j/               # Neo4j graph database
│   │   └── hsqldb/              # HSQLDB relational database
│   └── pipeline/                # Pipeline configurations
│       └── OmopDocRunner.piper  # Main pipeline configuration
├── data/
│   ├── input/                   # Input documents directory
│   └── output/                  # Output results directory
└── logs/                        # Application logs
```

## Quick Start

### 1. Build the Project

First, build the JAR file using Maven 3.8+:
Requires [dphe-nlp2](https://github.com/DeepPhe/dphe-nlp2) and [dphe-onto-db2](https://github.com/DeepPhe/dphe-onto-db2)

```bash
mvn clean package
```

### 2. Prepare Input Data

Place your clinical documents in the input director organized by patient ID:

```bash
mkdir -p data/input
# Copy your clinical text files to data/input/patient_id_1/docs
```

### 3. Run the Application

Use the provided deployment script for easy management:

```bash
# Make the script executable
chmod +x build-and-deploy.sh

# Build and run in foreground
./build-and-deploy.sh run

# Or run in background
./build-and-deploy.sh run-bg

# Process sample data (if available)
./build-and-deploy.sh sample
```

## Usage

### Deployment Script Commands

The `build-and-deploy.sh` script provides several convenient commands:

|  Command  | Description |
|-----------|-------------|
| `build`   | Build Docker image only |
| `run`     | Build and run application (foreground) |
| `run-bg`  | Build and run application (background) |
| `sample`  | Process sample data and show results |
| `stop`    | Stop all services |
| `status`  | Show service status and useful info |
| `logs`    | Show application logs |
| `cleanup` | Stop services and remove containers/volumes |
| `help`    | Show help message |

### Manual Docker Commands

If you prefer to use Docker directly:

```bash
# Build the image
docker compose build

# Run the application
docker compose up deepphe-omop

# Run in background
docker compose up -d deepphe-omop

# View logs
docker compose logs -f deepphe-omop

# Stop the application
docker compose down
```

### Input and Output

- **Input**: Place clinical text files in `data/input/`
- **Output**: Processed results will appear in `data/output/`
- **Logs**: Application logs are available in the `logs/` directory and via Docker logs

## Configuration

### Environment Variables

The application supports the following environment variables:

- `JAVA_OPTS`: JVM options (default: `-Xms512m -Xmx2048m -XX:+UseG1GC`)
- `APP_ENV`: Application environment (set to `docker` in container)

### Resource Limits

The Docker container is configured with:
- Memory limit: 3GB
- Memory reservation: 1GB

Adjust these limits in `docker-compose.yml` if needed based on your data volume and system resources.

### Pipeline Configuration

The main pipeline configuration is located at:
- `src/main/resources/pipeline/OmopDocRunner.piper`

This file contains paths to databases and other pipeline settings. The Docker setup automatically maps these to container-appropriate paths.

## Database Resources

The application includes embedded databases:

- **Neo4j**: Graph database containing the DeepPhe knowledge base (Embedded format)
- **HSQLDB**: Relational database for OMOP CDM storage (Embedded format)

These databases are automatically included in the Docker image and don't require separate setup.

## Monitoring and Troubleshooting

### Check Application Status

```bash
./build-and-deploy.sh status
```

### View Real-time Logs

```bash
./build-and-deploy.sh logs
```

### Debug Container Issues

```bash
# Shell into the running container
docker compose exec deepphe-omop sh

# Check container resource usage
docker stats deepphe-omop-app
```

### Common Issues

1. **JAR file not found**: Ensure you've built the project with `mvn clean package`
2. **Out of memory errors**: Increase memory limits in `docker-compose.yml`
3. **Empty output**: Check input file format and logs for processing errors
4. **Permission issues**: The container runs as a non-root user; ensure file permissions are correct

## Development

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd deepphe-omop

# Build the project
mvn clean package

# Build Docker image
docker compose build
```

### Modifying Configuration

1. Edit pipeline configuration in `src/main/resources/pipeline/OmopDocRunner.piper`
2. Adjust Docker settings in `docker-compose.yml`
3. Modify the deployment script `build-and-deploy.sh` for custom workflows

## Performance Considerations

- The application processes documents sequentially
- Memory usage scales with document size and complexity
- For large document sets, consider:
  - Increasing memory limits
  - Processing documents in batches
  - Using faster storage for input/output directories

## Security

- The container runs as a non-root user (`appuser`)
- Input directory is mounted read-only
- No network ports are exposed by default (unless your application requires it)

## Support

For issues and questions:
1. Check the application logs for error messages
2. Verify input file formats match expected requirements
3. Ensure adequate system resources are available
4. Review the pipeline configuration for path and database issues

## License

Apache 2.0

## Contributing

Please create a pull request with description for review
