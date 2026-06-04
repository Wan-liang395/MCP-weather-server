package year2026.month6.mcpweatherserver.core.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import year2026.month6.mcpweatherserver.core.model.McpRequest;
import year2026.month6.mcpweatherserver.core.model.McpResponse;
import year2026.month6.mcpweatherserver.core.registry.ToolRegistry;
import year2026.month6.mcpweatherserver.tool.base.McpTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class McpSseController {

    private final ToolRegistry toolRegistry;


    private final ObjectMapper objectMapper;

    // 维护所有正在连接的客户端，Key 是 sessionId
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();

    /**
     *  1.建立长连接
     * 大模型首先通过 GET 请求这里,保持连接不断开
     */
    @GetMapping(value = "/sse", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter connect(@RequestParam(defaultValue = "default-session") String sessionId,HttpServletResponse response){
        log.info("大模型客户端尝试建立连接，Session ID: {}", sessionId);

        response.setCharacterEncoding("UTF-8");

        // 设置超时时间，这里设为 1 小时
        SseEmitter emitter = new SseEmitter(3600000L);
        connections.put(sessionId, emitter);

        // 处理连接断开的情况，防止内存泄漏
        emitter.onCompletion(() -> connections.remove(sessionId));
        emitter.onTimeout(() -> connections.remove(sessionId));
        emitter.onError(e -> connections.remove(sessionId));

        try {
            // 按照 MCP 规范，连接成功后必须下发一条 endpoint 事件
            emitter.send(SseEmitter.event().name("endpoint").data("/mcp/message?sessionId=" + sessionId));
            log.info("SSE 连接建立成功！");
        } catch (IOException e) {
            connections.remove(sessionId);
            log.error("建立 SSE 连接失败", e);
        }

        return emitter;
    }

    /**
     *  2.接收指令并处理
     * 大模型通过 POST 将 JSON-RPC 请求发到这里
     */
    @PostMapping("/message")
    public ResponseEntity<Void> handleMessage(
            @RequestParam(defaultValue = "default-session") String sessionId,
            @RequestBody McpRequest request) {

        log.info("收到大模型指令: Method={}, ID={}", request.getMethod(), request.getId());

        SseEmitter emitter = connections.get(sessionId);
        if (emitter == null) {
            log.warn("Session {} 不存在或已断开", sessionId);
            return ResponseEntity.badRequest().build();
        }

        try {
            // 1.处理请求并打包结果
            McpResponse mcpResponse = processRequest(request);

            //2. 将结果通过刚才建立好的 SSE 通道推回给大模型

            String jsonString = objectMapper.writeValueAsString(mcpResponse);
            emitter.send(SseEmitter.event().name("message").data(jsonString));

            log.info("指令处理完毕，已推送结果回大模型。");
        } catch (Exception e) {
            log.error("处理消息时发生异常", e);
        }

        return ResponseEntity.accepted().build();
    }

    /**
     * 核心路由网关：根据 method 决定干什么活
     */
    @SuppressWarnings("unchecked")
    private McpResponse processRequest(McpRequest request) {
        String method = request.getMethod();
        McpResponse.McpResponseBuilder responseBuilder = McpResponse.builder().id(request.getId());

        // 1. 大模型想看看我们都有什么工具
        if ("tools/list".equals(method)) {
            List<Map<String, Object>> toolList = toolRegistry.getAllTools().stream()
                    .map(tool -> Map.of(
                            "name", tool.getName(),
                            "description", tool.getDescription(),
                            "inputSchema", tool.getInputSchema()
                    )).collect(Collectors.toList());

            responseBuilder.result(Map.of("tools", toolList));
        }
        // 2. 大模型决定调用某个具体工具
        else if ("tools/call".equals(method)) {
            Map<String, Object> params = request.getParams();
            String toolName = (String) params.get("name");
            Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");

            McpTool tool = toolRegistry.getTool(toolName);
            if (tool != null) {
                // 执行你的工具逻辑（比如查天气）
                String resultText = tool.execute(arguments);

                // 将文本结果包装成 MCP 标准要求的格式
                responseBuilder.result(Map.of(
                        "content", List.of(Map.of("type", "text", "text", resultText))
                ));
            } else {
                // 找不到工具时返回标准错误码
                responseBuilder.error(Map.of("code", -32601, "message", "Tool not found"));
            }
        }
        // 3. 处理握手请求 (Initialize)
        else if ("initialize".equals(method)) {
            responseBuilder.result(Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of("tools", Map.of()),
                    "serverInfo", Map.of("name", "java-weather-mcp", "version", "1.0.0")
            ));
        }
        else {
            responseBuilder.error(Map.of("code", -32601, "message", "Method not found"));
        }

        return responseBuilder.build();
    }

}
