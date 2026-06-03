package year2026.month6.mcpweatherserver.tool.base;

import java.util.Map;

/**
 * MCP 工具的基础接口
 * 所有提供给大模型使用的工具都必须实现此接口
 */
public interface McpTool {
    /**
     * 工具名称。
     */
    String getName();

    /**
     * 工具描述。
     */
    String getDescription();

    /**
     * 工具的入参定义 (严格符合 JSON Schema 规范)。
     */
    Map<String, Object> getInputSchema();

    /**
     * 核心执行逻辑。
     * @param arguments 大模型解析并传过来的具体参数
     * @return 返回给大模型的最终文本结果
     */
    String execute(Map<String, Object> arguments);
}
