package io.github.tokenless;

import com.google.gson.JsonSyntaxException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * tokenless 命令行工具
 * 用法: java -jar tokenless.jar &lt;input&gt; [-o &lt;output&gt;]
 */
public class TokenlessCli {

    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out, System.err));
    }

    static int run(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr) {
        try {
            return execute(args, stdin, stdout, stderr);
        } catch (Exception e) {
            String message = e.getMessage();
            stderr.println("错误: " + (message == null ? e.getClass().getSimpleName() : message));
            return 1;
        }
    }

    private static int execute(String[] args, InputStream stdin, PrintStream stdout, PrintStream stderr)
            throws IOException {
        if (args.length == 0 || hasFlag(args, "--help") || hasFlag(args, "-h")) {
            printHelp(stdout);
            return args.length == 0 ? 1 : 0;
        }

        String inputPath = null;
        String outputPath = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-o") || args[i].equals("--output")) {
                if (i + 1 >= args.length) {
                    stderr.println("错误: -o 选项需要指定输出文件路径");
                    return 1;
                }
                outputPath = args[++i];
            } else {
                if (inputPath != null) {
                    stderr.println("错误: 仅支持一个位置参数作为输入文件路径");
                    return 1;
                }
                inputPath = args[i];
            }
        }

        if (inputPath == null) {
            stderr.println("错误: 请指定输入文件路径");
            return 1;
        }

        String result;
        if (inputPath.equals("-")) {
            String content = readStdin(stdin);
            try {
                result = Tokenless.convertJson(content);
            } catch (JsonSyntaxException e) {
                result = Tokenless.convertMarkdown(content);
            }
        } else {
            result = Tokenless.tokenlessFile(inputPath);
        }

        if (outputPath != null) {
            Files.writeString(Path.of(outputPath), result, StandardCharsets.UTF_8);
        } else {
            stdout.println(result);
        }
        return 0;
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }

    private static String readStdin(InputStream stdin) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stdin, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static void printHelp(PrintStream stdout) {
        stdout.println("用法: java -jar tokenless.jar <input> [-o <output>]");
        stdout.println();
        stdout.println("参数:");
        stdout.println("  input          输入文件路径，或使用 - 从标准输入读取");
        stdout.println("  -o, --output   输出文件路径（默认输出到标准输出）");
    }
}
