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
        insertLog("\n-Programa detectado", null);
        while (!isAtEnd()) {
            parseStatement();
        }
    }

    private void parseStatement() {
        Token first = peek();

        if (first.type == TokenType.KEYWORD && validTypes.contains(first.value)) {
            parseDeclaration();
            expect(TokenType.SEPARATOR, ";");

        } else if (first.type == TokenType.IDENTIFIER) {
            parseAssignment();
            expect(TokenType.SEPARATOR, ";");

        } else if (first.type == TokenType.KEYWORD && first.value.equals("System.out.println")) {
            parsePrint();
            expect(TokenType.SEPARATOR, ";");

        } else if (first.type == TokenType.KEYWORD && first.value.equals("if")) {
            parseIf();

        } else if (first.type == TokenType.KEYWORD && first.value.equals("while")) {
            parseWhile();

        } else {
            insertLog("\n----------\nComando inválido iniciado por: " + first.value + "\n", errorStyle);
            error("Esperado declaração, atribuição, impressão, if ou while");
        }
    }


    private void parsePrint() {
        insertLog("\n-Comando de impressão detectado", null);

        // PRINT
        Token printToken = advance();
        insertLog("\n-Keyword detectada: " + printToken.value, null);

        // "("
        expect(TokenType.SEPARATOR, "(");
        insertLog("\n-Aberto parêntese '('", null);

        // Expressão dentro do print
        parseExpression();

        // ")"
        expect(TokenType.SEPARATOR, ")");
        insertLog("\n-Fechado parêntese ')'", null);
    }

    
    private void parseAssignment() {
        insertLog("\n-Atribuição detectada", null);

        // IDENTIFICADOR
        Token id = advance();
        insertLog("\n-Identificador detectado: " + id.value, null);

        // "="
        expect(TokenType.OPERATOR, "=");
        insertLog("\n-Operador '=' detectado", null);

        // Expressão
        parseExpression();
    }

    private void parseDeclaration() {
        insertLog("\n-Declaração detectada", null);
        parseType();
        expect(TokenType.IDENTIFIER, null);

        if (match(TokenType.OPERATOR, "=")) {
            insertLog("\n-Encontrou o '=', expressão detectada", null);
            parseExpression();
        }
    }

    private void parseType() {

        Token token = advance();
        
        insertLog("\n-Tipo detectado: " + token.value, null);
        
        if (!(token.type == TokenType.KEYWORD && validTypes.contains(token.value))) {
            insertLog("--------\nERRO!\nTipo inválido: " + token.value + "\n\n", errorStyle);
            error("Esperado tipo primitivo, encontrado: " + token.value);
        }
    }

    private void parseExpression() {
        insertLog("\n-Expressão detectada", null);
        parseTerm();
        // enquanto achar + ou -
        while (true) {
            if (match(TokenType.OPERATOR, "+") || match(TokenType.OPERATOR, "-")) {
                Token op = tokens.get(position - 1); // token do operador que foi consumido
                insertLog("\n-Operador aditivo detectado: " + op.value, null);
                parseTerm();
            } else {
                break;
            }
        }
    }

    private void parseTerm() {
        parseFactor();
        // enquanto achar * ou /
        while (true) {
            if (match(TokenType.OPERATOR, "*") || match(TokenType.OPERATOR, "/")) {
                Token op = tokens.get(position - 1);
                insertLog("\n-Operador multiplicativo detectado: " + op.value, null);
                parseFactor();
            } else {
                break;
            }
        }
    }

    private void parseFactor() {
        Token tok = peek();

        // número literal
        if (tok.type == TokenType.NUMBER) {
            Token num = advance();
            insertLog("\n-Número detectado: " + num.value, null);
            return;
        }

        // string literal
        if (tok.type == TokenType.STRING) {
            Token s = advance();
            insertLog("\n-String detectada: " + s.value, null);
            return;
        }

        // identificador
        if (tok.type == TokenType.IDENTIFIER) {
            Token id = advance();
            insertLog("\n-Identificador detectado: " + id.value, null);
            return;
        }

        // boolean literal (true/false) tokenizados como KEYWORD no seu lexer
        if (tok.type == TokenType.KEYWORD &&
            (tok.value.equals("true") || tok.value.equals("false"))) {
            Token b = advance();
            insertLog("\n-Boolean detectado: " + b.value, null);
            return;
        }

        // subexpressão entre parênteses
        if (match(TokenType.SEPARATOR, "(")) {
            insertLog("\n-Abertura de parênteses '('", null);
            parseExpression();
            expect(TokenType.SEPARATOR, ")");
            insertLog("\n-Fechamento de parênteses ')'", null);
            return;
        }

        // nenhum caso válido
        insertLog("\nFator inválido: " + tok.value + "\n", errorStyle);
        error("Fator inválido: " + tok.value);
    }
    
    private void parseIf() {
        insertLog("\n-Comando 'if' detectado", null);
        advance(); // consome o 'if'

        expect(TokenType.SEPARATOR, "(");
        parseCondition(); // <- agora usa condição
        expect(TokenType.SEPARATOR, ")");

        expect(TokenType.SEPARATOR, "{");
        while (!match(TokenType.SEPARATOR, "}")) {
            parseStatement();
        }

        // else opcional
        if (match(TokenType.KEYWORD, "else")) {
            expect(TokenType.SEPARATOR, "{");
            while (!match(TokenType.SEPARATOR, "}")) {
                parseStatement();
            }
        }
    }

    private void parseWhile() {
        insertLog("\n-Comando 'while' detectado", null);
        advance(); // consome o 'while'

        expect(TokenType.SEPARATOR, "(");
        parseCondition(); // <- agora usa condição
        expect(TokenType.SEPARATOR, ")");

        expect(TokenType.SEPARATOR, "{");
        while (!match(TokenType.SEPARATOR, "}")) {
            parseStatement();
        }
    }

    private void parseCondition() {
        insertLog("\n-Condição detectada", null);

        // Expressão da esquerda
        parseExpression();

        // Verifica operador relacional sem avançar por engano
        Token opToken = peek();
        boolean isRelOp = opToken.type == TokenType.OPERATOR &&
            (opToken.value.equals("==") || opToken.value.equals("!=") ||
             opToken.value.equals("<")  || opToken.value.equals(">")  ||
             opToken.value.equals("<=") || opToken.value.equals(">="));

        if (!isRelOp) {
            insertLog("\nOperador relacional inválido: " + opToken.value + "\n", errorStyle);
            error("Esperado operador relacional, encontrado: " + opToken.value);
        }

        // consome o operador relacional (sabemos que é válido)
        Token op = advance();
        insertLog("\n-Operador relacional detectado: " + op.value, null);

        // Expressão da direita
        parseExpression();
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
        
        insertLog("\n-verificando token: " + (value != null ? value : type), null);
        
        if (!match(type, value)) {
            insertLog("\n----------\nToken esperado: " + (value != null ? value : type) + "\n", errorStyle);
            error("\nEsperado token " + (value != null ? value : type));
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

