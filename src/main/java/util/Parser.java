/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package util;

/**
 *
 * @author david
 */

import java.util.List;
import java.util.Set;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import util.analisadorLexico.TokenType;

public class Parser {
    private final List<Token> tokens;
    
    private int position = 0;
    private JTextPane log;
    
    private StyledDocument doc;
    private Style defaultStyle;
    private Style errorStyle;
    
    
    // Tipos primitivos aceitos para declaração
    private static final Set<String> validTypes = Set.of("int", "float", "boolean", "String");

    public Parser(List<Token> tokens, JTextPane log) {
        this.tokens = tokens;
        this.log = log;
        
        // Inicializa o documento e estilos
        this.doc = log.getStyledDocument();

        this.errorStyle = log.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, java.awt.Color.RED);
    }

    public void parse() {
        insertLog("Iniciando parse...\n", null);
        parseProgram();

        if (!isAtEnd()) {
            insertLog("Tokens inesperados após o fim do programa.\n", errorStyle);
            error("Tokens inesperados após o fim do programa.");
        }
    }

    private void parseProgram() {
        while (!isAtEnd()) {
            parseStatement();
        }
    }

    private void parseStatement() {
        parseDeclaration();
        expect(TokenType.SEPARATOR, ";");
    }

    private void parseDeclaration() {
        insertLog("parse declaração...\n", null);
        parseType();
        expect(TokenType.IDENTIFIER, null);

        if (match(TokenType.OPERATOR, "=")) {
            parseExpression();
        }
    }

    private void parseType() {
        insertLog("parse tipo...\n", null);

        Token token = advance();
        if (!(token.type == TokenType.KEYWORD && validTypes.contains(token.value))) {
            insertLog("--------\nERRO!\nTipo inválido: " + token.value + "\n\n", errorStyle);
            error("Esperado tipo primitivo, encontrado: " + token.value);
        }
    }

    private void parseExpression() {
        Token token = advance();
        boolean isValid =
                token.type == TokenType.NUMBER ||
                token.type == TokenType.STRING ||
                token.type == TokenType.IDENTIFIER ||
                (token.type == TokenType.KEYWORD && (token.value.equals("true") || token.value.equals("false")));

        if (!isValid) {
            insertLog("Expressão inválida: " + token.value + "\n", errorStyle);
            error("Expressão inválida: " + token.value);
        }
    }

    // Utilitários
    private boolean isAtEnd() {
        return position >= tokens.size();
    }

    private Token advance() {
        if (isAtEnd()) {
            insertLog("Fim inesperado dos tokens.\n", errorStyle);
            error("Fim inesperado dos tokens.");
        }
        return tokens.get(position++);
    }

    private Token peek() {
        if (isAtEnd()) {
            insertLog("Fim inesperado dos tokens.\n", errorStyle);
            error("Fim inesperado dos tokens.");
        }
        return tokens.get(position);
    }

    private boolean match(TokenType type, String value) {
        if (isAtEnd()) return false;
        Token token = peek();
        if (token.type != type) return false;
        if (value != null && !token.value.equals(value)) return false;
        advance();
        return true;
    }

    private void expect(TokenType type, String value) {
        if (!match(type, value)) {
            insertLog("Esperado token " + (value != null ? value : type) + "\n", errorStyle);
            error("Esperado token " + (value != null ? value : type));
        }
    }

    private void error(String message) {
        throw new RuntimeException("Erro de parsing: " + message);
    }

    private void insertLog(String text, Style style) {
        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}

