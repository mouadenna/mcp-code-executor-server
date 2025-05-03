# MCP Code Executor Server

A robust Model Context Protocol (MCP) server that enables AI agents to execute code across multiple programming languages in a secure, isolated environment.

## Overview

This project implements a Model Context Protocol (MCP) server using Spring AI to provide code execution capabilities to AI agents. Think of MCP like a USB-C port for AI applications - it standardizes how AI models connect to different data sources and tools. This code executor server helps bridge the gap between language models and actual code execution.

## Key Features

- **Multi-language Support**: Execute code in Java, Python, JavaScript, TypeScript, and C++
- **Secure Execution**: Runs code in isolated environments with proper resource constraints
- **MCP Integration**: Connects seamlessly with any MCP-compatible client and build your own agent capable of running code
- **Error Handling**: Provides detailed feedback for compilation and runtime errors
- **Resource Management**: Automatically cleans up temporary files and enforces execution timeouts

## How It Works

The server exposes a standardized MCP tool endpoint that allows AI assistants to:

1. Submit code in a supported language
2. Have it executed in a controlled environment
3. Receive the execution output or error messages

For compiled languages (Java, C++, TypeScript), the server automatically handles the compilation step before execution.

## Getting Started

### Prerequisites

- Java 17 or higher
- Python (for Python code execution)
- Node.js and npm (for JavaScript/TypeScript)
- g++ or compatible C++ compiler (for C++ code execution)

### Running Locally

```bash
# Clone the repository
git clone https://github.com/yourusername/mcp-code-executor-server.git
cd mcp-code-executor-server

# Build the project
./mvnw clean package

# Run the server
./mvnw spring-boot:run
```

The server will start on port 8080 by default and register itself as an MCP server capable of executing code.

### Connecting to MCP Clients

1. Start your MCP client
2. Connect to the MCP server at `http://localhost:8080`
3. The client will discover the available code execution tool
4. You can now ask the AI to execute code through the MCP server

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

## MCP Architecture

This project follows the Model Context Protocol's client-server architecture:

- **MCP Host**: AI applications like Claude Desktop that want to execute code
- **MCP Client**: The protocol client that connects to our server
- **MCP Server**: This application (exposes code execution capabilities through MCP)
- **Resources**: The code execution service exposed as a standardized MCP tool

## Security Considerations

For production use, consider implementing:

- More restrictive resource limits (CPU, memory, execution time)
- Stronger sandboxing for execution environments
- Input validation and sanitization
- Authentication and authorization for MCP connections
- Network isolation for executed code

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details. 