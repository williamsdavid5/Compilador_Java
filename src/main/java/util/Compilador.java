/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.JTextArea;

public class Compilador {
    
    private PrintStream printStream;
    private JTextArea terminal;
    
    public Compilador(PrintStream printStream, JTextArea terminal) {
        this.printStream = printStream;
        this.terminal = terminal;
    }

    
    public void compilar(String codigo) throws Exception {
        terminal.append("Compilando na JVM...\nSaída:\n\n");
        
        File codigoFile = new File("MinhaClasse.java");
        
        // Adiciona um main em volta do código digitado
        String mainString =
            "public class MinhaClasse {\n" +
            "    public static void main(String[] args) {\n" +
            codigo + "\n" +
            "    }\n" +
            "}";

        try (FileWriter writer = new FileWriter(codigoFile)) {
            writer.write(mainString);
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JVM não tem compilador. Rode no JDK e não no JRE!");
        }

        int result = compiler.run(null, null, null, codigoFile.getPath());
        if (result != 0) {
            terminal.append("Erro!\n");
            throw new RuntimeException("Erro na compilação!");
        }

        // Carrega a classe compilada
        URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File(".").toURI().toURL() });
        Class<?> cls = Class.forName("MinhaClasse", true, classLoader);

        // Invoca o main
        Method main = cls.getDeclaredMethod("main", String[].class);
        String[] args = new String[]{};
        main.invoke(null, (Object) args);
    }
}

