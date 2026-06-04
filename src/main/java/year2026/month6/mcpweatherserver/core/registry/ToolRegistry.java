package year2026.month6.mcpweatherserver.core.registry;

import year2026.month6.mcpweatherserver.tool.base.McpTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册中心
 * 负责在 Spring 启动时自动收集并动态管理所有的 MCP 插件工具
 */
@Component
public class ToolRegistry {

    // 缓存池：Key 为工具名称，Value 为具体的工具实现类
    private final Map<String, McpTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 构造器注入：Spring 启动时,会自动把所有带 @Component 且实现了 McpTool 的类
     * 打包成一个 List 塞进这个构造函数里,我们不需要手动一个一个去 new
     */
    public ToolRegistry(List<McpTool> tools) {
        for (McpTool tool : tools) {
            toolMap.put(tool.getName(), tool);
        }
    }

    /**
     * 获取所有已注册的工具
     * 场景：大模型发起 "tools/list" 请求时调用,向大模型展示我们有哪些能力
     */
    public List<McpTool> getAllTools() {
        return List.copyOf(toolMap.values());
    }

    /**
     * 根据名称获取特定工具
     * 场景：大模型发起 "tools/call" 请求时,根据大模型指定的 toolName 路由到对应的处理类
     */
    public McpTool getTool(String name) {
        return toolMap.get(name);
    }
}