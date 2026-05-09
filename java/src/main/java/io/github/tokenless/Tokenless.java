package io.github.tokenless;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * tokenless - 一行代码降低60%的token量
 * 将JSON和Markdown格式转换成tokenless格式
 */
public class Tokenless {
    
    private static final Gson gson = new Gson();
    
    /**
     * 读取本地文件并转换为tokenless格式
     *
     * 根据文件扩展名自动判断文件类型：
     * - .json 文件：解析为JSON对象后转换
     * - .md / .markdown 文件：作为Markdown文本转换
     * - 其他文件：先尝试解析为JSON，失败则作为Markdown文本转换
     *
     * @param filePath 本地文件路径
     * @return tokenless格式字符串
     * @throws IOException 文件读取失败时抛出
     * @throws JsonSyntaxException 当 .json 文件内容不是合法 JSON 时抛出
     */
    public static String tokenlessFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".json")) {
            return convertJson(content);
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return convertMarkdown(content);
        }
        // 其他扩展名：尝试JSON，失败则当Markdown处理
        if (!looksLikeJsonValue(content.trim())) {
            return convertMarkdown(content);
        }
        try {
            return convertJson(parseJson(content));
        } catch (JsonSyntaxException e) {
            return convertMarkdown(content);
        }
    }

    /**
     * 主入口函数，自动检测输入类型并转换
     * @param input 输入数据（JSON字符串或Markdown字符串）
     * @return tokenless格式字符串
     */
    public static String tokenless(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String trimmed = input.trim();
        // 判断是否为JSON
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return convertJson(trimmed);
        }
        return convertMarkdown(input);
    }
    
    /**
     * 转换JSON字符串
     */
    public static String convertJson(String jsonStr) {
        return convertJson(parseJson(jsonStr));
    }

    private static String convertJson(JsonElement element) {
        return convertJsonElement(element, 0);
    }

    private static JsonElement parseJson(String jsonStr) {
        JsonReader reader = new JsonReader(new StringReader(jsonStr));
        reader.setLenient(false);
        return JsonParser.parseReader(reader);
    }

    private static boolean looksLikeJsonValue(String text) {
        if (text.isEmpty()) {
            return false;
        }
        char first = text.charAt(0);
        if (first == '{' || first == '[' || first == '"' || first == '-' || Character.isDigit(first)) {
            return true;
        }
        if (first == 't') {
            return text.startsWith("true");
        }
        if (first == 'f') {
            return text.startsWith("false");
        }
        if (first == 'n') {
            return text.startsWith("null");
        }
        return false;
    }
    
    /**
     * 转换Java对象为tokenless格式
     */
    public static String convertObject(Object obj) {
        JsonElement element = gson.toJsonTree(obj);
        return convertJsonElement(element, 0);
    }
    
    private static String convertJsonElement(JsonElement element, int indent) {
        String prefix = " ".repeat(indent);
        
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            // 检查是否为对象数组（可转CSV）
            if (isObjectArray(array)) {
                return convertObjectArrayToCsv(array, indent);
            }
            // 普通数组
            List<String> lines = new ArrayList<>();
            for (JsonElement item : array) {
                if (item.isJsonObject()) {
                    lines.add(prefix + "-");
                    lines.add(convertJsonElement(item, indent + 1));
                } else {
                    lines.add(prefix + "-" + formatValue(item));
                }
            }
            return String.join("\n", lines);
        }
        
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                if (value.isJsonObject() || value.isJsonArray()) {
                    lines.add(prefix + key);
                    lines.add(convertJsonElement(value, indent + 1));
                } else {
                    lines.add(prefix + key + ":" + formatValue(value));
                }
            }
            return String.join("\n", lines);
        }
        
        return formatValue(element);
    }

    
    private static boolean isObjectArray(JsonArray array) {
        if (array.isEmpty()) return false;
        for (JsonElement item : array) {
            if (!item.isJsonObject()) return false;
            JsonObject obj = item.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonObject() || value.isJsonArray()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    private static String convertObjectArrayToCsv(JsonArray array, int indent) {
        if (array.isEmpty()) return "";
        String prefix = " ".repeat(indent);
        
        // 获取所有键
        Set<String> keys = new LinkedHashSet<>();
        for (JsonElement item : array) {
            keys.addAll(item.getAsJsonObject().keySet());
        }
        List<String> keyList = new ArrayList<>(keys);
        
        // 生成CSV
        List<String> lines = new ArrayList<>();
        lines.add(prefix + String.join(",", keyList));
        for (JsonElement item : array) {
            JsonObject obj = item.getAsJsonObject();
            List<String> values = new ArrayList<>();
            for (String key : keyList) {
                values.add(formatValue(obj.get(key)));
            }
            lines.add(prefix + String.join(",", values));
        }
        return String.join("\n", lines);
    }
    
    private static String formatValue(JsonElement element) {
        if (element == null || element.isJsonNull()) return "";
        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? "1" : "0";
            }
            return primitive.getAsString();
        }
        return element.toString();
    }
    
    /**
     * 转换Markdown文本
     */
    public static String convertMarkdown(String text) {
        if (text == null) return "";
        String result = text;
        
        // 1. 处理表格
        result = convertMarkdownTable(result);
        
        // 2. 去掉标题格式
        result = result.replaceAll("(?m)^#{1,6}\\s+", "");
        
        // 3. 去掉加粗和斜体
        result = result.replaceAll("\\*{3}([^*]+)\\*{3}", "$1");
        result = result.replaceAll("\\*{2}([^*]+)\\*{2}", "$1");
        result = result.replaceAll("\\*([^*]+)\\*", "$1");
        
        // 4. 分割线替换为单个换行
        result = result.replaceAll("\\n*---+\\n*", "\n");
        
        // 5. 多个换行转成一个换行
        result = result.replaceAll("\\n{2,}", "\n");
        
        // 6. 多个空格/制表符转成一个空格
        result = result.replaceAll("[ \\t]+", " ");
        
        return result.trim();
    }
    
    private static String convertMarkdownTable(String text) {
        Pattern pattern = Pattern.compile("\\|(.+)\\|\\n\\|[-|\\s]+\\|\\n((?:\\|.+\\|\\n?)+)");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String headerRow = matcher.group(1);
            String bodyRows = matcher.group(2);
            
            // 解析表头
            String[] headers = Arrays.stream(headerRow.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
            
            // 解析数据行
            List<String> csvLines = new ArrayList<>();
            csvLines.add(String.join(",", headers));
            
            for (String row : bodyRows.trim().split("\\n")) {
                String[] cells = Arrays.stream(row.split("\\|"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toArray(String[]::new);
                csvLines.add(String.join(",", cells));
            }
            
            matcher.appendReplacement(sb, String.join("\n", csvLines));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
