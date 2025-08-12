/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

import java.util.*;
import java.util.regex.*;

/**
 *
 * @author david
 */
public class analisadorLexico {
    //identificadores de tokens indicam qual o tipo daquele token
    public enum TokenType {
        KEYWORD, IDENTIFIER, NUMBER, STRING, OPERATOR, SEPARATOR, COMMENT, WHITESPACE, UNKNOWN
    }
    
    // objeto/classe token
    //um token possui seu valor e seu identificador, que é o tipo

    
    //lista de palavras do java
    private static final Set<String> keywords = Set.of(
           "int", "float", "if", "else", "for", "while", "return", "public", "class", "static", "void", "new", "String", "boolean", "true", "false" 
    );
    
    //função que faz o reconhecimento
    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();
        
        // Regex básica para quebrar o código
        //este objeto funciona como um automato
        //se baseando na regex enviada, ele cria algo que funcione como ela (ou seja, um autonomo)
        Pattern pattern = Pattern.compile(
            "(//.*)|" +                      // Comentários
            "(\".*?\")|" +                   // Strings
            "\\b\\d+\\b|" +                  // Números
            "[a-zA-Z_][a-zA-Z_0-9]*|" +      // Identificadores/palavras-chave
            "[+\\-*/=<>!&|]+" +              // Operadores
            "|[(){};.,]"                     // Separadores
        );
        
        Matcher matcher = pattern.matcher(input); //matcher é o mecanismo responsável por passar o texto pela regex
        
        System.out.println(matcher);
        
        //é apenas aqui que o texto é passado na regex
        while (matcher.find()) {
            String token = matcher.group(); //a ocorrencia atual é "puxada" por esse metodo
            //na proxima repetição, será a outra ocorrencia
            
            //cada vez que algo é encontrado, é verificado onde se encaixa
            //startsWith é para termos em que precismos verificar apenas o inicio
            //comentários por exemplo, é apenas duas barras seguidas de qualquer coisa
            if (token.startsWith("//")) {
                tokens.add(new Token(TokenType.COMMENT, token));
                
            } else if (token.startsWith("\"")) {
                tokens.add(new Token(TokenType.STRING, token));
                
            } else if (token.matches("\\d+")) { //matches verifica o trecho inteiro de uma vez
                tokens.add(new Token(TokenType.NUMBER, token));
                
            } else if (keywords.contains(token)) {
                tokens.add(new Token(TokenType.KEYWORD, token));
                
            } else if (token.matches("[a-zA-Z_][a-zA-Z_0-9]*")) {
                tokens.add(new Token(TokenType.IDENTIFIER, token));
                
            } else if (token.matches("[+\\-*/=<>!&|]+")) {
                tokens.add(new Token(TokenType.OPERATOR, token));
                
            } else if (token.matches("[(){};.,]")) {
                tokens.add(new Token(TokenType.SEPARATOR, token));
                
            } else {
                tokens.add(new Token(TokenType.UNKNOWN, token));
                
            }
        }
        
        return tokens;
    }
}
