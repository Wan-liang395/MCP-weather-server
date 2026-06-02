package year2026.month6.mcpweatherserver.core.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class McpResponse {
    @Builder.Default
    private String jsonRpc = "2.0";

    private String id;

    // 执行成功时返回的数据
    private Object result;

    // 执行失败时返回的错误信息
    private Object error;
}
