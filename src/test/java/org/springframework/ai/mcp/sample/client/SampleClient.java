/*
* Copyright 2024 - 2024 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.springframework.ai.mcp.sample.client;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * @author Christian Tzolov
 */

public class SampleClient {

	private final McpClientTransport transport;

	public SampleClient(McpClientTransport transport) {
		this.transport = transport;
	}

	public void run() {

		var client = McpClient.sync(this.transport).build();

		client.initialize();

		client.ping();

		// List and demonstrate tools
		ListToolsResult toolsList = client.listTools();
		System.out.println("Available Tools = " + toolsList);

		// Test code execution with Java
		String javaCode = "public class HelloWorld {\n" +
						  "    public static void main(String[] args) {\n" +
						  "        System.out.println(\"Hello from MCP Code Executor!\");\n" +
						  "        System.out.println(\"The sum of 5 and 7 is: \" + (5 + 7));\n" +
						  "    }\n" +
						  "}";
		
		CallToolResult javaExecutionResult = client.callTool(new CallToolRequest("executeCode", 
				Map.of("language", "java", "code", javaCode)));
		System.out.println("Java Code Execution Result: " + javaExecutionResult);
		
		// Test code execution with Python
		String pythonCode = "print('Hello from Python!')\n" +
							"result = 10 * 3.5\n" +
							"print(f'10 multiplied by 3.5 equals {result}')";
		
		CallToolResult pythonExecutionResult = client.callTool(new CallToolRequest("executeCode", 
				Map.of("language", "python", "code", pythonCode)));
		System.out.println("Python Code Execution Result: " + pythonExecutionResult);

		client.closeGracefully();
	}
}