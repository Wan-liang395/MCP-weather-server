package year2026.month6.mcpweatherserver.tool.impl;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    private String apiKey;

    @Override
    public String getName() {
        return "get_realtime_weather_and_news";
    }

    @Override
    public String getDescription() {
        return "获取指定城市的实时天气情况，并支持联网搜索相关的出行建议和新闻。";
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
        log.info("接收到大模型指令，正在联网查询真实天气，目标城市：{}", location);
        try {
            // 1. 拼接目标 API 的 URL（直接使用中文城市名）
            String url = String.format(
                    "https://api.seniverse.com/v3/weather/now.json?key=%s&location=%s&language=zh-Hans&unit=c",
                    apiKey, location
            );

            // 2. 发起 GET 请求，并拿回原始 JSON 字符串
            String responseJson = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            // 3. 解析复杂的 JSON 树结构，精准提取需要的数据
            JsonNode rootNode = objectMapper.readTree(responseJson);
            // 按照 API 返回格式逐层深入：results[0] -> now -> text/temperature
            JsonNode weatherNode = rootNode.path("results").get(0).path("now");

            String text = weatherNode.path("text").asText(); // 天气现象，比如"多云"、"雷阵雨"
            String temp = weatherNode.path("temperature").asText(); // 摄氏度

            String finalResult = String.format("【实时联网天气】%s当前的真实天气是：%s，气温：%s°C。", location, text, temp);
            log.info("API 调用成功：{}", finalResult);

            return finalResult;

        } catch (Exception e) {
            log.error("调用外部天气 API 失败，可能是网络或秘钥问题", e);
            return "【系统提示】抱歉，调用真实天气接口失败，请检查服务器网络或 API 额度。";
        }
    }
}
