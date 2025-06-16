#!/bin/bash

# build-and-deploy.sh
# Script to build and deploy the DeepPhe OMOP application with Docker

set -e

echo "🚀 Starting DeepPhe OMOP Dockerization Process..."

# Configuration
APP_NAME="deepphe-omop"
JAR_NAME="deepphe-omop-0.1.0.jar"
JAR_PATH="target/$JAR_NAME"

# Function to check if required files exist
check_prerequisites() {
    echo "📋 Checking prerequisites..."
    
    if [ ! -f "$JAR_PATH" ]; then
        echo "❌ Error: JAR file '$JAR_PATH' not found!"
        echo "Please build your project first with: mvn clean package"
        exit 1
    fi
    
    if [ ! -d "src/main/resources/dphe-db-resources" ]; then
        echo "❌ Error: Database resources directory not found!"
        echo "Expected: src/main/resources/dphe-db-resources/"
        exit 1
    fi
    
    if [ ! -f "src/main/resources/pipeline/OmopDocRunner.piper" ]; then
        echo "❌ Error: Piper configuration file not found!"
        echo "Expected: src/main/resources/pipeline/OmopDocRunner.piper"
        exit 1
    fi
    
    # Create data directories if they don't exist
    mkdir -p data/input data/output
    
    echo "✅ All prerequisites met!"
}

# Function to setup directory structure
setup_directories() {
    echo "📁 Setting up directory structure..."
    
    # Create necessary directories
    mkdir -p data/input data/output logs
    
    # Set appropriate permissions
    chmod 755 data/input data/output
    
    echo "✅ Directory structure ready!"
}

# Function to validate piper configuration
check_piper_config() {
    echo "🔍 Checking piper configuration..."
    
    PIPER_FILE="src/main/resources/pipeline/OmopDocRunner.piper"
    
    if grep -q "/projects/" "$PIPER_FILE"; then
        echo "⚠️  Warning: Found hardcoded '/projects/' paths in piper file!"
        echo "💡 Consider updating paths in $PIPER_FILE to use Docker paths:"
        echo "   GraphDb=/app/resources/dphe-db-resources/neo4j/DeepPhe_2023_v1.db"
        echo "   deepphe_2023_v1_url=jdbc:hsqldb:file:/app/resources/dphe-db-resources/hsqldb/DeepPhe_2023_v1/DeepPhe_2023_v1"
        echo ""
    fi
}

# Function to build the application
build_app() {
    echo "🔨 Building Docker image..."
    docker compose build deepphe-omop
    echo "✅ Docker image built successfully!"
}

# Function to run the application
run_app() {
    echo "🚀 Starting DeepPhe OMOP application..."
    
    # Check if input directory has files
    if [ -z "$(ls -A data/input 2>/dev/null)" ]; then
        echo "⚠️  Warning: Input directory (data/input) is empty!"
        echo "💡 Add your input files to data/input/ before running"
        echo "Press Enter to continue anyway, or Ctrl+C to abort..."
        read
    fi
    
    docker compose up deepphe-omop
}

# Function to run in background
run_background() {
    echo "🚀 Starting DeepPhe OMOP application in background..."
    docker compose up -d deepphe-omop
    
    echo "⏳ Waiting for application to start..."
    sleep 10
    
    show_status
    
    echo ""
    echo "📋 Monitor progress with:"
    echo "  docker compose logs -f deepphe-omop"
}

# Function to show service status
show_status() {
    echo ""
    echo "📊 Service Status:"
    docker compose ps
    echo ""
    
    if docker compose ps | grep -q "Up"; then
        echo "📁 Data Directories:"
        echo "  • Input:  $(pwd)/data/input"
        echo "  • Output: $(pwd)/data/output"
        echo ""
        echo "📋 Useful Commands:"
        echo "  • View logs: docker compose logs -f deepphe-omop"
        echo "  • Stop service: docker compose down"
        echo "  • Shell into container: docker compose exec deepphe-omop sh"
        echo "  • Check output: ls -la data/output/"
        
        if docker compose --profile with-fileserver ps | grep -q "file-server.*Up"; then
            echo "  • File server: http://localhost:8080/output/"
        fi
    fi
}

# Function to stop services
stop_services() {
    echo "🛑 Stopping services..."
    docker compose down
    echo "✅ Services stopped!"
}

# Function to clean up everything
cleanup() {
    echo "🧹 Cleaning up..."
    docker compose down -v --rmi local
    docker system prune -f
    echo "✅ Cleanup complete!"
}

# Function to show logs
show_logs() {
    docker compose logs -f ${1:-deepphe-omop}
}

# Function to process sample data
process_sample() {
    echo "📄 Processing sample data..."
    
    run_background
    
    echo "⏳ Waiting for processing to complete..."
    
    # Wait for the container to finish processing
    while docker compose ps | grep -q "deepphe-omop.*Up"; do
        sleep 5
        echo -n "."
    done
    
    echo ""
    echo "✅ Processing complete! Check data/output/ for results."
    ls -la data/output/
}

# Main execution
case "${1:-help}" in
    "build")
        check_prerequisites
        setup_directories
        build_app
        ;;
    "run")
        check_prerequisites
        setup_directories
        check_piper_config
        build_app
        run_app
        ;;
    "run-bg"|"background")
        check_prerequisites
        setup_directories
        check_piper_config
        build_app
        run_background
        ;;
    "sample")
        check_prerequisites
        setup_directories
        check_piper_config
        build_app
        process_sample
        ;;
    "stop")
        stop_services
        ;;
    "status")
        show_status
        ;;
    "logs")
        show_logs ${2}
        ;;
    "cleanup")
        cleanup
        ;;
    "help")
        echo "Usage: $0 {build|run|run-bg|run-with-server|sample|stop|status|logs|cleanup|help}"
        echo ""
        echo "Commands:"
        echo "  build           - Build Docker image only"
        echo "  run             - Build and run application (foreground)"
        echo "  run-bg          - Build and run application (background)"
        echo "  sample          - Process sample data and show results"
        echo "  stop            - Stop all services"
        echo "  status          - Show service status and useful info"
        echo "  logs            - Show logs (optional: specify service name)"
        echo "  cleanup         - Stop services and remove containers/volumes"
        echo "  help            - Show this help message"
        echo ""
        echo "Data Directories:"
        echo "  • Place input files in: data/input/"
        echo "  • Output files will be in: data/output/"
        ;;
    *)
        echo "Unknown command: $1"
        echo "Use '$0 help' for usage information"
        exit 1
        ;;
esac