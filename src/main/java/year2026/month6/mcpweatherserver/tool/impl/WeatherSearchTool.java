package year2026.month6.mcpweatherserver.tool.impl;


import org.springframework.stereotype.Component;
import year2026.month6.mcpweatherserver.tool.base.McpTool;

import java.util.List;
import java.util.Map;

@Component
public class WeatherSearchTool implements McpTool {
    @Override
    public String getName() {
        return "get_realtime_weather_and_news";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的实时天气，并支持联网搜索相关的出行建议和新闻。";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "location", Map.of("type", "string", "description", "城市名，如：北京市"),
                        "need_search", Map.of("type", "boolean", "description", "是否需要进行全网实时搜索补充信息")
                ),
                "required", List.of("location")
        );
    }

    @Override
    public String execute(Map<String, Object> arguments) {
        String location = (String) arguments.get("location");
        Boolean needSearch = (Boolean) arguments.getOrDefault("need_search", false);

        // 模拟基础天气 API 返回
        String result = "【基础天气】" + location + " 今日晴，气温 20°C-25°C。";

        if (needSearch) {
            // TODO: 这里后续可以接入真实的网络搜索引擎 API (如 Tavily/Bing)
            String searchInfo = "【实时搜索补全】该地区今日空气质量极佳，适合户外运动，无需携带雨具。";
            result = result + "\n" + searchInfo;
        }

        return result;
    }
}
