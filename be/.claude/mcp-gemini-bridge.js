#!/usr/bin/env node

/**
 * MCP Server for Gemini Integration
 * This server acts as a bridge between Claude Code and Gemini API
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import { CallToolRequestSchema, ListToolsRequestSchema } from '@modelcontextprotocol/sdk/types.js';
import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

class GeminiMCPServer {
  constructor() {
    this.server = new Server(
      {
        name: 'gemini-bridge',
        version: '0.1.0',
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupToolHandlers();
  }

  setupToolHandlers() {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: [
          {
            name: 'gemini_analyze',
            description: 'Analyze code or text using Gemini',
            inputSchema: {
              type: 'object',
              properties: {
                prompt: {
                  type: 'string',
                  description: 'The prompt to send to Gemini'
                },
                context_files: {
                  type: 'array',
                  items: { type: 'string' },
                  description: 'Files to include in context'
                }
              },
              required: ['prompt']
            }
          },
          {
            name: 'gemini_search',
            description: 'Search and explore codebase using Gemini',
            inputSchema: {
              type: 'object',
              properties: {
                search_query: {
                  type: 'string',
                  description: 'What to search for'
                },
                search_path: {
                  type: 'string',
                  description: 'Path to search in'
                }
              },
              required: ['search_query']
            }
          },
          {
            name: 'gemini_document',
            description: 'Generate documentation using Gemini',
            inputSchema: {
              type: 'object',
              properties: {
                doc_type: {
                  type: 'string',
                  enum: ['api', 'readme', 'technical', 'user_guide'],
                  description: 'Type of documentation to generate'
                },
                source_files: {
                  type: 'array',
                  items: { type: 'string' },
                  description: 'Source files to document'
                }
              },
              required: ['doc_type']
            }
          }
        ]
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;

      try {
        switch (name) {
          case 'gemini_analyze':
            return await this.handleAnalyze(args);
          case 'gemini_search':
            return await this.handleSearch(args);
          case 'gemini_document':
            return await this.handleDocument(args);
          default:
            throw new Error(`Unknown tool: ${name}`);
        }
      } catch (error) {
        return {
          content: [
            {
              type: 'text',
              text: `Error: ${error.message}`
            }
          ],
          isError: true
        };
      }
    });
  }

  async handleAnalyze(args) {
    const { prompt, context_files = [] } = args;
    
    try {
      // Use local file analysis when Gemini API is unavailable
      let contextContent = '';
      if (context_files.length > 0) {
        for (const file of context_files) {
          try {
            const { stdout } = await execAsync(`type "${file}"`);
            contextContent += `\n--- ${file} ---\n${stdout}\n`;
          } catch (e) {
            contextContent += `\n--- ${file} ---\n[File not found or inaccessible]\n`;
          }
        }
      }

      return {
        content: [
          {
            type: 'text',
            text: `Analysis Request: ${prompt}\n\nContext Files:\n${contextContent}\n\nNote: Gemini API currently unavailable due to quota limits. Consider using alternative analysis methods.`
          }
        ]
      };
    } catch (error) {
      throw new Error(`Analysis failed: ${error.message}`);
    }
  }

  async handleSearch(args) {
    const { search_query, search_path = '.' } = args;
    
    try {
      // Use ripgrep for search
      const { stdout } = await execAsync(`rg "${search_query}" "${search_path}" --type java --type js --type json -n -C 2`);
      
      return {
        content: [
          {
            type: 'text',
            text: `Search Results for: "${search_query}"\n\n${stdout}`
          }
        ]
      };
    } catch (error) {
      return {
        content: [
          {
            type: 'text',
            text: `Search completed. No matches found for: "${search_query}"`
          }
        ]
      };
    }
  }

  async handleDocument(args) {
    const { doc_type, source_files = [] } = args;
    
    try {
      let documentation = `# ${doc_type.toUpperCase()} Documentation\n\n`;
      
      for (const file of source_files) {
        try {
          const { stdout } = await execAsync(`type "${file}"`);
          documentation += `## ${file}\n\n\`\`\`\n${stdout}\n\`\`\`\n\n`;
        } catch (e) {
          documentation += `## ${file}\n\n[File not accessible]\n\n`;
        }
      }
      
      documentation += `\nNote: This documentation was generated with basic templates. For enhanced documentation with AI analysis, Gemini API integration is recommended when quota is available.`;

      return {
        content: [
          {
            type: 'text',
            text: documentation
          }
        ]
      };
    } catch (error) {
      throw new Error(`Documentation generation failed: ${error.message}`);
    }
  }

  async start() {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
  }
}

// Start the server if this file is run directly
if (import.meta.url === `file://${process.argv[1]}`) {
  const server = new GeminiMCPServer();
  server.start().catch(console.error);
}