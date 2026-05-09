#!/usr/bin/env node
/**
 * tokenless CLI - 命令行工具
 * 用法: tokenless <input> [-o <output>]
 */

import { tokenlessFile, tokenless } from './index.js';
import { readFileSync, writeFileSync } from 'fs';

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
    outputPath = args[++i];
  } else {
    inputPath = args[i];
  }
}

if (!inputPath) {
  console.error('错误: 请指定输入文件路径');
  process.exit(1);
}

let result;
if (inputPath === '-') {
  const content = readFileSync('/dev/stdin', 'utf-8');
  result = tokenless(content);
} else {
  result = tokenlessFile(inputPath);
}

if (outputPath) {
  writeFileSync(outputPath, result, 'utf-8');
} else {
  process.stdout.write(result + '\n');
}
