"""
tokenless Python SDK 测试
"""

import json
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(ROOT))
from tokenless import tokenless, tokenless_file

# 加载测试用例
test_cases_path = Path(__file__).parent.parent.parent / 'test-cases.json'
with open(test_cases_path, 'r', encoding='utf-8') as f:
    test_cases = json.load(f)['testCases']


def test_all_cases():
    """运行所有测试用例"""
    passed = 0
    failed = 0
    
    print('=== tokenless Python SDK 测试 ===\n')
    
    for tc in test_cases:
        result = tokenless(tc['input'])
        expected = tc['expected']
        success = result == expected
        
        if success:
            print(f"✓ {tc['name']}")
            passed += 1
        else:
            print(f"✗ {tc['name']}")
            print(f"  期望: {repr(expected)}")
            print(f"  实际: {repr(result)}")
            failed += 1

    file_cases = [
        ('JSON 文件处理', 'sample.json', '{"a":1,"b":true}', 'a:1\nb:1'),
        ('Markdown 文件处理', 'sample.md', '# Title\n\n**bold**', 'Title\nbold'),
        ('其他扩展名 JSON 回退处理', 'sample.txt', '{"a":1}', 'a:1'),
        ('其他扩展名 Markdown 回退处理', 'sample.txt', '# Heading', 'Heading'),
    ]

    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        for name, file_name, content, expected in file_cases:
            path = tmp_path / file_name
            path.write_text(content, encoding='utf-8')
            result = tokenless_file(path)
            success = result == expected

            if success:
                print(f"✓ {name}")
                passed += 1
            else:
                print(f"✗ {name}")
                print(f"  期望: {repr(expected)}")
                print(f"  实际: {repr(result)}")
                failed += 1

        invalid_json_path = tmp_path / 'invalid.json'
        invalid_json_path.write_text('{', encoding='utf-8')
        try:
            tokenless_file(invalid_json_path)
            print("✗ 非法 JSON 文件异常处理")
            print("  期望: 抛出 JSONDecodeError")
            print("  实际: 未抛出异常")
            failed += 1
        except json.JSONDecodeError:
            print("✓ 非法 JSON 文件异常处理")
            passed += 1

    cli_result = subprocess.run(
        [sys.executable, '-m', 'tokenless', '-'],
        input='{"a":1,"b":true}',
        text=True,
        capture_output=True,
        cwd=ROOT,
        check=True,
    )
    if cli_result.stdout == 'a:1\nb:1\n':
        print("✓ Python CLI 标准输入 JSON 处理")
        passed += 1
    else:
        print("✗ Python CLI 标准输入 JSON 处理")
        print("  期望: 'a:1\\nb:1\\n'")
        print(f"  实际: {repr(cli_result.stdout)}")
        failed += 1
    
    print(f"\n总计: {passed} 通过, {failed} 失败")
    return failed == 0


if __name__ == '__main__':
    success = test_all_cases()
    sys.exit(0 if success else 1)
