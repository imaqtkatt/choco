package com.github.imaqtkatt;

import com.github.imaqtkatt.lang.compiler.Compiler;
import com.github.imaqtkatt.lang.lexer.Lexer;
import com.github.imaqtkatt.lang.parser.Parser;
import com.github.imaqtkatt.lang.tree.Program;
import com.github.imaqtkatt.lang.typed.Environment;
import com.github.imaqtkatt.lang.typed.infer.InferNode;
import com.github.imaqtkatt.lang.typed.infer.InferProgram;
import com.github.imaqtkatt.lang.typed.tree.Node;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        assert (args.length == 1);

        var filePath = args[0];
        var lexer = Lexer.fromFile(new File(filePath));
        var parser = new Parser(lexer);
        var program = parser.program();
        var programT = InferProgram.inferProgram(program);
        var compiler = new Compiler(programT);
        var bytes = compiler.compile();
        var packageName = program.packageName();

        var outputPath = "./%s/Main.class".formatted(packageName);
        var output = new File(outputPath);
        try (var outputStream = new FileOutputStream(output)) {
            outputStream.write(bytes);
            outputStream.flush();
        }

        var root = new File(".");
        var rootUrl = root.toURI().toURL();
        var here = new File("./build/classes/main");
        var hereUrl = here.toURI().toURL();
        URL[] urls = {rootUrl, hereUrl};

        try (var dcl = new DynamicClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            Class<?> clazz = dcl.loadClass("example.Main");
            System.out.println("clazz.getDeclaredMethods() = " + Arrays.toString(clazz.getDeclaredMethods()));
            Method adder = clazz.getDeclaredMethod("increase");
            Object result1 = adder.invoke(null);
            System.out.println("result = " + result1);
            Object result2 = adder.invoke(null);
            System.out.println("result = " + result2);
        }
    }
}
