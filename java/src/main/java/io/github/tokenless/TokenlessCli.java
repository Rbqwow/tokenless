package io.github.tokenless;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * tokenless 命令行工具
 * 用法: java -jar tokenless.jar &lt;input&gt; [-o &lt;output&gt;]
 */
public class TokenlessCli {

    public static void main(String[] args) throws Exception {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printHelp();
            System.exit(args.length == 0 ? 1 : 0);
        }

        String inputPath = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o") || args[i].equals("--output")) {
                if (i + 1 >= args.length) {
                    System.err.println("错误: -o 选项需要指定输出文件路径");
                    System.exit(1);
                }
                outputPath = args[++i];
            } else {
                inputPath = args[i];
            }
        }

        if (inputPath == null) {
            System.err.println("错误: 请指定输入文件路径");
            System.exit(1);
        }

        String result;
        if (inputPath.equals("-")) {
            // 从标准输入读取
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            result = Tokenless.tokenless(sb.toString());
        } else {
            result = Tokenless.tokenlessFile(inputPath);
        }

        if (outputPath != null) {
            Files.writeString(Path.of(outputPath), result, StandardCharsets.UTF_8);
        } else {
            System.out.println(result);
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("用法: java -jar tokenless.jar <input> [-o <output>]");
        System.out.println();
        System.out.println("参数:");
        System.out.println("  input          输入文件路径，或使用 - 从标准输入读取");
        System.out.println("  -o, --output   输出文件路径（默认输出到标准输出）");
    }
}
