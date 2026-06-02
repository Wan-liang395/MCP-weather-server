package year2026.month6.mcpweatherserver.core.model;

import lombok.Data;
import java.util.Map;

/**
 * 接收客户端 (大模型) 发来的 MCP 请求
 */
@Data
public class McpRequest {
    // 强制协议版本
    private String jsonRpc = "2.0";
    // 消息的唯一 ID（由客户端生成，服务端需原样返回）
    private String id;
    //要调用的方法
    private String method;
    //具体的参数集合
    private Map<String, Object> params;
}
