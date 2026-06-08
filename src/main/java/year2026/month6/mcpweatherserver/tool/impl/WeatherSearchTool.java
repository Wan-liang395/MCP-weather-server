package year2026.month6.mcpweatherserver.tool.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import year2026.month6.mcpweatherserver.tool.base.McpTool;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherSearchTool implements McpTool {

    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    @Value("${weather.api.key}")
    private String weatherApiKey;

    @Value("${tavily.api.key}")
    private String tavilyApiKey;

    @Override
    public String getName() {
        return "get_realtime_weather_and_news";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的实时天气情况。当用户询问与天气相关的具体出行建议、航班高速状况、突发新闻时，请将 need_search 设为 true 以获取最新全网资讯。";
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
        log.info("执行工具：城市={}, 是否需要联网搜索={}", location, needSearch);
        String weatherData = fetchBaseWeather(location);

        // 2. 如果大模型认为有必要，触发全网搜索兜底
        if (needSearch) {
            String searchData = fetchRealtimeNewsFromWeb(location);
            return weatherData + searchData;
        }

        return weatherData;
    }

    private String fetchBaseWeather(String location){
            try {
            // 1. 拼接目标 API 的 URL（直接使用中文城市名）
            String url = String.format(
                    "https://api.seniverse.com/v3/weather/now.json?key=%s&location=%s&language=zh-Hans&unit=c",
                    weatherApiKey, location
            );
                String responseJson = restClient.get().uri(url).retrieve().body(String.class);
                JsonNode rootNode = objectMapper.readTree(responseJson);
                JsonNode weatherNode = rootNode.path("results").get(0).path("now");
                return String.format("【基础天气】%s：当前天气%s，气温%s°C。",
                        location, weatherNode.path("text").asText(), weatherNode.path("temperature").asText());

        } catch (Exception e) {
                log.error("基础天气 API 异常", e);
                return "【基础天气】暂时无法获取准确气温。";
            }
    }
    private String fetchRealtimeNewsFromWeb(String location) {
        String query = location + " 今日天气新闻 突发路况 穿衣出行建议";
        log.info("触发全网实时搜索，搜索词：{}", query);

        try {
            // 构建 Tavily 请求体
            Map<String, Object> requestBody = Map.of(
                    "api_key", tavilyApiKey,
                    "query", query,
                    "search_depth", "basic",
                    "max_results", 3  // 抓取前 3 条最相关的全网结果
            );

            // 发送 POST 请求
            String responseJson = restClient.post()
                    .uri("https://api.tavily.com/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 解析 Tavily 结果
            JsonNode resultsNode = objectMapper.readTree(responseJson).path("results");
            StringBuilder newsBuilder = new StringBuilder();
            newsBuilder.append("\n\n【全网实时搜索补充】：\n");

            for (JsonNode node : resultsNode) {
                String title = node.path("title").asText();
                String content = node.path("content").asText(); // 这里已经是干干净净的文本了
                newsBuilder.append("- ").append(title).append("\n  ").append(content).append("\n");
            }

            return newsBuilder.toString();
        } catch (Exception e) {
            log.error("全网搜索失败", e);
            return "\n\n【全网实时搜索补充】：当前网络波动，无法获取最新资讯。";
        }

    }
}
