# tokenless Java SDK

一行代码降低60%的token量。

## 安装

Maven:
```xml
<dependency>
    <groupId>io.github.tokenless</groupId>
    <artifactId>tokenless</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 使用方法

```java
import io.github.tokenless.Tokenless;

// 自动检测输入类型并转换
String result = Tokenless.tokenless(data);
```

## 命令行工具

构建 fat jar 后可直接在命令行使用：

```bash
# 构建
mvn package

# 转换文件并输出到标准输出
java -jar target/tokenless.jar before.md

# 转换文件并保存到指定路径
java -jar target/tokenless.jar before.md -o after.md

# 从标准输入读取
cat data.json | java -jar target/tokenless.jar -
```

## API

### Tokenless.tokenless(String input)

主入口函数，自动检测输入类型并转换。

- 输入JSON字符串时，转换为tokenless格式
- 输入Markdown字符串时，去除冗余格式

```java
// JSON对象数组 -> CSV格式
String json = "[{\"name\":\"张三\",\"age\":25},{\"name\":\"李四\",\"age\":30}]";
Tokenless.tokenless(json);
// 输出:
// name,age
// 张三,25
// 李四,30

// Markdown -> 纯文本
Tokenless.tokenless("# 标题\n**重点**内容");
// 输出: 标题\n重点内容
```

### Tokenless.tokenlessFile(String filePath)

读取本地文件并转换为tokenless格式。根据文件扩展名自动判断类型：`.json` 文件解析为JSON，`.md` / `.markdown` 文件作为Markdown处理，其他文件先尝试JSON，失败则当Markdown处理。

```java
// 处理JSON文件
String result = Tokenless.tokenlessFile("data.json");

// 处理Markdown文件
String result2 = Tokenless.tokenlessFile("README.md");
```

### Tokenless.convertJson(String jsonStr)

仅转换JSON字符串。

### Tokenless.convertObject(Object obj)

转换Java对象为tokenless格式。

### Tokenless.convertMarkdown(String text)

仅转换Markdown文本。

## 运行测试

```bash
mvn test
```
