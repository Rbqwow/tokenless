#!/usr/bin/env node
/**
 * tokenless CLI - 命令行工具
 * 用法: tokenless <input> [-o <output>]
 */

import { tokenlessFile, tokenless, convertJson } from './index.js';
import { writeFileSync } from 'fs';

const args = process.argv.slice(2);

if (args.length === 0 || args.includes('--help') || args.includes('-h')) {
  console.log('用法: tokenless <input> [-o <output>]');
  console.log('');
  console.log('参数:');
  console.log('  input          输入文件路径，或使用 - 从标准输入读取');
  console.log('  -o, --output   输出文件路径（默认输出到标准输出）');
  process.exit(args.length === 0 ? 1 : 0);
}

// 解析参数
let inputPath = null;
let outputPath = null;

for (let i = 0; i < args.length; i++) {
  if (args[i] === '-o' || args[i] === '--output') {
    if (i + 1 >= args.length) {
      console.error('错误: -o 选项需要指定输出文件路径');
      process.exit(1);
    }
    outputPath = args[++i];
  } else {
    inputPath = args[i];
  }
}

if (!inputPath) {
  console.error('错误: 请指定输入文件路径');
  process.exit(1);
}

async function run() {
  let result;
  if (inputPath === '-') {
    // 从标准输入读取（跨平台）
    const chunks = [];
    for await (const chunk of process.stdin) {
      chunks.push(chunk);
    }
    const content = Buffer.concat(chunks).toString('utf-8');
    try {
      result = convertJson(JSON.parse(content));
    } catch {
      result = tokenless(content);
    }
  } else {
    result = tokenlessFile(inputPath);
  }

  if (outputPath) {
    writeFileSync(outputPath, result, 'utf-8');
  } else {
    process.stdout.write(result + '\n');
  }
}

run().catch(err => {
  console.error('错误:', err.message);
  process.exit(1);
});
