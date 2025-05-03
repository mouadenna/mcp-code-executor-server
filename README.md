# Spring AI MCP Code Executor

This project demonstrates the use of Spring AI's Model Context Protocol (MCP) to provide a code execution service. The service allows AI Agents to execute code in multiple programming languages through a structured API offering a dedicated isolated environment for your agent.

## Features

- Execute code in multiple languages
- Handles compilation for compiled languages
- Clean and simple API
- Provides a dedicated isolated environment for secure code execution
- Supported languages:
  - Java
  - Python
  - JavaScript
  - TypeScript
  - C++

## How It Works

The application uses Spring Boot and Spring AI to expose code execution capabilities through the Model Context Protocol. The key components are:

1. **CodeExecutionService**: A service that creates temporary files with the provided code, executes them in the appropriate runtime, and returns the output. For compiled languages, it handles the compilation step automatically.

2. **McpServerApplication**: The main Spring Boot application that registers the CodeExecutionService as a tool provider for AI systems.

## Containerized Deployment

For improved security and isolation, this application can be deployed as a Docker container:

### Using Docker

```bash
# Build the Docker image
docker build -t mcp-code-executor .

# Run the container
docker run -p 8080:8080 mcp-code-executor
```

### Using Docker Compose

```bash
# Start the service
docker-compose up -d

# Check logs
docker-compose logs -f

# Stop the service
docker-compose down
```

The Docker setup includes:
- A non-root user for improved security
- Resource limits to prevent abuse
- Isolated execution environment
- Ephemeral storage for code execution

## Safety Features

- Execution timeouts (15 seconds by default)
- Temporary file cleanup
- Combined stdout/stderr output
- Error handling and compilation feedback

## Usage

To run the application:

```bash
./mvnw spring-boot:run
```

The MCP service will be available for AI systems to connect to and execute code.

## API

The service exposes the following tool endpoint through MCP:

- `executeCode(String language, String code)`: Execute code in the specified language

## Requirements

- Java 17 or higher
- Python (for Python code execution)
- Node.js (for JavaScript and TypeScript code execution)
- g++ or compatible C++ compiler (for C++ code execution)
- TypeScript tools (for TypeScript code execution)

## Security Considerations

This application is designed for demonstration purposes. In a production environment, consider:

- Adding resource limitations
- Implementing security sandboxing
- Adding authentication and authorization
- Restricting execution to trusted code only 