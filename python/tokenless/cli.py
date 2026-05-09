"""
tokenless CLI - 命令行工具
用法: tokenless <input> [-o <output>]
"""

import argparse
import json
import sys
from pathlib import Path

from . import convert_json, convert_markdown, tokenless_file


def main():
    parser = argparse.ArgumentParser(
        prog='tokenless',
        description='将JSON/Markdown文件转换为tokenless格式',
    )
    parser.add_argument(
        'input',
        help='输入文件路径，或使用 - 从标准输入读取',
    )
    parser.add_argument(
        '-o', '--output',
        metavar='OUTPUT',
        help='输出文件路径（默认输出到标准输出）',
    )
    args = parser.parse_args()

    if args.input == '-':
        content = sys.stdin.read()
        try:
            result = convert_json(json.loads(content))
        except json.JSONDecodeError:
            result = convert_markdown(content)
    else:
        result = tokenless_file(args.input)

    if args.output:
        Path(args.output).write_text(result, encoding='utf-8')
    else:
        print(result)


if __name__ == '__main__':
    main()
