# tokenless JavaScript SDK

一行代码降低60%的token量。

## 安装

```bash
npm install tokenless
```

## 使用方法

```javascript
import { tokenless } from 'tokenless';

// 自动检测输入类型并转换
const result = tokenless(data);
```

## 命令行工具

安装后可直接在命令行使用：

```bash
# 转换文件并输出到标准输出
tokenless before.md

# 转换文件并保存到指定路径
tokenless before.md -o after.md

# 从标准输入读取
cat data.json | tokenless -
```

## API

### tokenless(input)

主入口函数，自动检测输入类型并转换。

- 输入JSON对象/数组时，转换为tokenless格式
- 输入Markdown字符串时，去除冗余格式

```javascript
// JSON对象数组 -> CSV格式
const users = [
  { name: '张三', age: 25, city: '北京' },
  { name: '李四', age: 30, city: '上海' }
];
tokenless(users);
// 输出:
// name,age,city
// 张三,25,北京
// 李四,30,上海

// 嵌套JSON -> 简化YAML格式
const data = {
  user: { name: '张三', age: 25 },
  active: true
};
tokenless(data);
// 输出:
// user
//  name:张三
//  age:25
// active:1

// Markdown -> 纯文本
tokenless('# 标题\n**重点**内容');
// 输出: 标题\n重点内容
```

### tokenlessFile(filePath)

读取本地文件并转换为tokenless格式。根据文件扩展名自动判断类型：`.json` 文件解析为JSON，`.md` / `.markdown` 文件作为Markdown处理，其他文件先尝试JSON，失败则当Markdown处理。

```javascript
import { tokenlessFile } from 'tokenless';

// 处理JSON文件
const result = tokenlessFile('data.json');

// 处理Markdown文件
const result2 = tokenlessFile('README.md');
```

### convertJson(data, indent?)

仅转换JSON数据。

### convertMarkdown(text)

仅转换Markdown文本。

## 运行测试

```bash
npm test
```
