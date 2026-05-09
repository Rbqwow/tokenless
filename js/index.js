/**
 * tokenless - 一行代码降低60%的token量
 * 将JSON和Markdown格式转换成tokenless格式
 */

import { readFileSync } from 'fs';
import { extname } from 'path';

/**
 * 读取本地文件并转换为tokenless格式
 *
 * 根据文件扩展名自动判断文件类型：
 * - .json 文件：解析为JSON对象后转换
 * - .md / .markdown 文件：作为Markdown文本转换
 * - 其他文件：先尝试解析为JSON，失败则作为Markdown文本转换
 *
 * @param {string} filePath - 本地文件路径
 * @returns {string} - tokenless格式字符串
 */
export function tokenlessFile(filePath) {
  const content = readFileSync(filePath, 'utf-8');
  const ext = extname(filePath).toLowerCase();
  if (ext === '.json') {
    return convertJson(JSON.parse(content));
  }
  if (ext === '.md' || ext === '.markdown') {
    return convertMarkdown(content);
  }
  // 其他扩展名：尝试JSON，失败则当Markdown处理
  try {
    return convertJson(JSON.parse(content));
  } catch {
    return convertMarkdown(content);
  }
}

/**
 * 主入口函数，自动检测输入类型并转换
 * @param {any} input - 输入数据（JSON对象/数组或Markdown字符串）
 * @returns {string} - tokenless格式字符串
 */
export function tokenless(input) {
  if (typeof input === 'string') {
    return convertMarkdown(input);
  }
  return convertJson(input);
}

/**
 * 转换JSON数据
 * @param {any} data - JSON对象或数组
 * @param {number} indent - 缩进层级
 * @returns {string}
 */
export function convertJson(data, indent = 0) {
  const prefix = ' '.repeat(indent);
  
  // 处理对象数组（表格类数据）
  if (isObjectArray(data)) {
    return convertObjectArrayToCsv(data, indent);
  }
  
  // 处理普通数组
  if (Array.isArray(data)) {
    return data.map(item => {
      if (typeof item === 'object' && item !== null) {
        return `${prefix}-\n${convertJson(item, indent + 1)}`;
      }
      return `${prefix}-${formatValue(item)}`;
    }).join('\n');
  }
  
  // 处理对象
  if (typeof data === 'object' && data !== null) {
    const lines = [];
    for (const [key, value] of Object.entries(data)) {
      if (typeof value === 'object' && value !== null) {
        lines.push(`${prefix}${key}`);
        lines.push(convertJson(value, indent + 1));
      } else {
        lines.push(`${prefix}${key}:${formatValue(value)}`);
      }
    }
    return lines.join('\n');
  }
  
  return formatValue(data);
}

/**
 * 判断是否为对象数组（可转为CSV）
 */
function isObjectArray(data) {
  if (!Array.isArray(data) || data.length === 0) return false;
  return data.every(item => 
    typeof item === 'object' && 
    item !== null && 
    !Array.isArray(item) &&
    Object.values(item).every(v => typeof v !== 'object' || v === null)
  );
}

/**
 * 将对象数组转换为CSV格式
 */
function convertObjectArrayToCsv(data, indent = 0) {
  if (data.length === 0) return '';
  const prefix = ' '.repeat(indent);
  const keys = Object.keys(data[0]);
  const header = prefix + keys.join(',');
  const rows = data.map(item => prefix + keys.map(k => formatValue(item[k])).join(','));
  return [header, ...rows].join('\n');
}

/**
 * 格式化值
 */
function formatValue(value) {
  if (value === true) return '1';
  if (value === false) return '0';
  if (value === null || value === undefined) return '';
  return String(value);
}


/**
 * 转换Markdown文本
 * @param {string} text - Markdown文本
 * @returns {string}
 */
export function convertMarkdown(text) {
  let result = text;
  
  // 1. 处理表格
  result = convertMarkdownTable(result);
  
  // 2. 去掉标题格式 (# ## ### 等)
  result = result.replace(/^#{1,6}\s+/gm, '');
  
  // 3. 去掉加粗和斜体 (***text***, **text**, *text*)
  result = result.replace(/\*{3}([^*]+)\*{3}/g, '$1');
  result = result.replace(/\*{2}([^*]+)\*{2}/g, '$1');
  result = result.replace(/\*([^*]+)\*/g, '$1');
  
  // 4. 分割线替换为单个换行
  result = result.replace(/\n*---+\n*/g, '\n');
  
  // 5. 多个换行转成一个换行
  result = result.replace(/\n{2,}/g, '\n');
  
  // 6. 多个空格/制表符转成一个空格
  result = result.replace(/[ \t]+/g, ' ');
  
  // 清理首尾空白
  result = result.trim();
  
  return result;
}

/**
 * 转换Markdown表格为CSV格式
 */
function convertMarkdownTable(text) {
  const tableRegex = /\|(.+)\|\n\|[-|\s]+\|\n((?:\|.+\|\n?)+)/g;
  
  return text.replace(tableRegex, (match, headerRow, bodyRows) => {
    // 解析表头
    const headers = headerRow.split('|').map(h => h.trim()).filter(h => h);
    
    // 解析数据行
    const rows = bodyRows.trim().split('\n').map(row => {
      return row.split('|').map(cell => cell.trim()).filter(cell => cell);
    });
    
    // 生成CSV
    const csvLines = [headers.join(',')];
    rows.forEach(row => csvLines.push(row.join(',')));
    
    return csvLines.join('\n');
  });
}

export default tokenless;
