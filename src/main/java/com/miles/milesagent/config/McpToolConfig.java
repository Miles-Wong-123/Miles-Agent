package com.miles.milesagent.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP 工具配置。
 * MCP 可以理解为一种“让模型调用外部工具服务”的协议层。
 */
@Configuration
public class McpToolConfig {

    /**
     * 智谱开放平台 API Key，用于访问远程 MCP 搜索服务。
     */
    @Value("${bigmodel.api-key}")
    private String apiKey;

    @Bean
    public McpToolProvider mcpToolProvider() {
        // 创建一个基于 HTTP SSE 的 MCP 传输层，当前接的是 web_search 工具。
        McpTransport searchTransport = new HttpMcpTransport.Builder()
                // trim 是为了避免配置文件里的 key 前后误带空格导致鉴权失败。
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + apiKey.trim())
                .build();

        // 把传输层包装成 MCP Client，LangChain4j 会通过它发现并调用远程工具。
        McpClient searchClient = new DefaultMcpClient.Builder()
                .key("BigModelSearchMcpClient")
                .transport(searchTransport)
                .build();


//        // 下面这段是本地时间 MCP 工具的示例，目前被注释掉了，没有参与运行。
//        McpTransport timeTransport = new StdioMcpTransport.Builder()
//                .command(Arrays.asList("uvx", "mcp-server-time", "--local-timezone=Asia/Shanghai"))
//                .build();
//
//        McpClient timeClient = new DefaultMcpClient.Builder()
//                .key("timeClient")
//                .transport(timeTransport)
//                .build();
//
//
//        return McpToolProvider.builder()
//                .mcpClients(Arrays.asList(timeClient, searchClient))
//                .build();

        // 当前只启用了搜索工具客户端。
        return McpToolProvider.builder()
                .mcpClients(searchClient)
                .build();
    }
}
