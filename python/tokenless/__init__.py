"""
tokenless - 一行代码降低60%的token量
将JSON和Markdown格式转换成tokenless格式
"""

import json
import re
from pathlib import Path
from typing import Any, List, Dict, Union

__version__ = "1.0.0"


def tokenless_file(file_path: Union[str, Path]) -> str:
    """
    读取本地文件并转换为tokenless格式
    
    根据文件扩展名自动判断文件类型：
    - .json 文件：解析为JSON对象后转换
    - .md / .markdown 文件：作为Markdown文本转换
    - 其他文件：先尝试解析为JSON，失败则作为Markdown文本转换
    
    Args:
        file_path: 本地文件路径（字符串或Path对象）
    
    Returns:
        tokenless格式字符串
    
    Raises:
        FileNotFoundError: 文件不存在时抛出
    """
    path = Path(file_path)
    content = path.read_text(encoding='utf-8')
    ext = path.suffix.lower()
    if ext == '.json':
        return convert_json(json.loads(content))
    if ext in ('.md', '.markdown'):
        return convert_markdown(content)
    # 其他扩展名：尝试JSON，失败则当Markdown处理
    try:
        return convert_json(json.loads(content))
    except (json.JSONDecodeError, ValueError):
        return convert_markdown(content)


def tokenless(input_data: Any) -> str:
    """
    主入口函数，自动检测输入类型并转换
    
    Args:
        input_data: 输入数据（JSON对象/数组或Markdown字符串）
    
    Returns:
        tokenless格式字符串
    """
    if isinstance(input_data, str):
        return convert_markdown(input_data)
    return convert_json(input_data)


def convert_json(data: Any, indent: int = 0) -> str:
    """
    转换JSON数据
    
    Args:
        data: JSON对象或数组
        indent: 缩进层级
    
    Returns:
        tokenless格式字符串
    """
    prefix = ' ' * indent
    
    # 处理对象数组（表格类数据）
    if _is_object_array(data):
        return _convert_object_array_to_csv(data, indent)
    
    # 处理普通数组
    if isinstance(data, list):
        lines = []
        for item in data:
            if isinstance(item, dict):
                lines.append(f"{prefix}-")
                lines.append(convert_json(item, indent + 1))
            else:
                lines.append(f"{prefix}-{_format_value(item)}")
        return '\n'.join(lines)
    
    # 处理对象
    if isinstance(data, dict):
        lines = []
        for key, value in data.items():
            if isinstance(value, (dict, list)):
                lines.append(f"{prefix}{key}")
                lines.append(convert_json(value, indent + 1))
            else:
                lines.append(f"{prefix}{key}:{_format_value(value)}")
        return '\n'.join(lines)
    
    return _format_value(data)


def _is_object_array(data: Any) -> bool:
    """判断是否为对象数组（可转为CSV）"""
    if not isinstance(data, list) or len(data) == 0:
        return False
    return all(
        isinstance(item, dict) and
        all(not isinstance(v, (dict, list)) for v in item.values())
        for item in data
    )


def _convert_object_array_to_csv(data: List[Dict], indent: int = 0) -> str:
    """将对象数组转换为CSV格式"""
    if not data:
        return ''
    prefix = ' ' * indent
    keys = list(data[0].keys())
    header = prefix + ','.join(keys)
    rows = [prefix + ','.join(_format_value(item.get(k, '')) for k in keys) for item in data]
    return '\n'.join([header] + rows)


def _format_value(value: Any) -> str:
    """格式化值"""
    if value is True:
        return '1'
    if value is False:
        return '0'
    if value is None:
        return ''
    return str(value)


def convert_markdown(text: str) -> str:
    """
    转换Markdown文本
    
    Args:
        text: Markdown文本
    
    Returns:
        tokenless格式字符串
    """
    result = text
    
    # 1. 处理表格
    result = _convert_markdown_table(result)
    
    # 2. 去掉标题格式 (# ## ### 等)
    result = re.sub(r'^#{1,6}\s+', '', result, flags=re.MULTILINE)
    
    # 3. 去掉加粗和斜体 (***text***, **text**, *text*)
    result = re.sub(r'\*{3}([^*]+)\*{3}', r'\1', result)
    result = re.sub(r'\*{2}([^*]+)\*{2}', r'\1', result)
    result = re.sub(r'\*([^*]+)\*', r'\1', result)
    
    # 4. 分割线替换为单个换行
    result = re.sub(r'\n*---+\n*', '\n', result)
    
    # 5. 多个换行转成一个换行
    result = re.sub(r'\n{2,}', '\n', result)
    
    # 6. 多个空格/制表符转成一个空格
    result = re.sub(r'[ \t]+', ' ', result)
    
    return result.strip()


def _convert_markdown_table(text: str) -> str:
    """转换Markdown表格为CSV格式"""
    table_pattern = r'\|(.+)\|\n\|[-|\s]+\|\n((?:\|.+\|\n?)+)'
    
    def replace_table(match):
        header_row = match.group(1)
        body_rows = match.group(2)
        
        # 解析表头
        headers = [h.strip() for h in header_row.split('|') if h.strip()]
        
        # 解析数据行
        rows = []
        for row in body_rows.strip().split('\n'):
            cells = [cell.strip() for cell in row.split('|') if cell.strip()]
            rows.append(cells)
        
        # 生成CSV
        csv_lines = [','.join(headers)]
        for row in rows:
            csv_lines.append(','.join(row))
        
        return '\n'.join(csv_lines)
    
    return re.sub(table_pattern, replace_table, text)
