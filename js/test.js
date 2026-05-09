/**
 * tokenless JavaScript SDK 测试
 */

import { tokenless, tokenlessFile } from './index.js';
import { execFileSync } from 'child_process';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'fs';
import { tmpdir } from 'os';
import { join } from 'path';
import { fileURLToPath } from 'url';

// 加载测试用例
const testCases = JSON.parse(readFileSync('../test-cases.json', 'utf-8')).testCases;

let passed = 0;
let failed = 0;

console.log('=== tokenless JavaScript SDK 测试 ===\n');

for (const tc of testCases) {
  const result = tokenless(tc.input);
  const success = result === tc.expected;
  
  if (success) {
    console.log(`✓ ${tc.name}`);
    passed++;
  } else {
    console.log(`✗ ${tc.name}`);
    console.log(`  期望: ${JSON.stringify(tc.expected)}`);
    console.log(`  实际: ${JSON.stringify(result)}`);
    failed++;
  }
}

const tmpDir = mkdtempSync(join(tmpdir(), 'tokenless-js-'));

try {
  const fileCases = [
    ['JSON 文件处理', 'sample.json', '{"a":1,"b":true}', 'a:1\nb:1'],
    ['Markdown 文件处理', 'sample.md', '# Title\n\n**bold**', 'Title\nbold'],
    ['其他扩展名 JSON 回退处理', 'sample.txt', '{"a":1}', 'a:1'],
    ['其他扩展名 Markdown 回退处理', 'sample.markdown.txt', '# Heading', 'Heading'],
  ];

  for (const [name, fileName, content, expected] of fileCases) {
    const filePath = join(tmpDir, fileName);
    writeFileSync(filePath, content, 'utf-8');
    const result = tokenlessFile(filePath);
    const success = result === expected;

    if (success) {
      console.log(`✓ ${name}`);
      passed++;
    } else {
      console.log(`✗ ${name}`);
      console.log(`  期望: ${JSON.stringify(expected)}`);
      console.log(`  实际: ${JSON.stringify(result)}`);
      failed++;
    }
  }

  const cliOutput = execFileSync('node', ['cli.js', '-'], {
    cwd: fileURLToPath(new URL('.', import.meta.url)),
    input: '{"a":1,"b":true}',
    encoding: 'utf-8',
  });

  if (cliOutput === 'a:1\nb:1\n') {
    console.log('✓ JavaScript CLI 标准输入 JSON 处理');
    passed++;
  } else {
    console.log('✗ JavaScript CLI 标准输入 JSON 处理');
    console.log('  期望: "a:1\\nb:1\\n"');
    console.log(`  实际: ${JSON.stringify(cliOutput)}`);
    failed++;
  }
} finally {
  rmSync(tmpDir, { recursive: true, force: true });
}

console.log(`\n总计: ${passed} 通过, ${failed} 失败`);
process.exit(failed > 0 ? 1 : 0);
