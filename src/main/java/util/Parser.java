package util;

import java.util.List;
import java.util.Set;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import util.analisadorLexico.TokenType;

public class Parser {
    private final List<Token> tokens;
    
    private int position = 0;
    private JTextPane log;
    private JTree arvoreDerivacao;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    
    private StyledDocument doc;
    private Style defaultStyle;
    private Style errorStyle;
    
    // Tipos primitivos aceitos para declaração
    private static final Set<String> validTypes = Set.of("int", "float", "boolean", "String");

    public Parser(List<Token> tokens, JTextPane log, JTree arvoreDerivacao) {
        this.tokens = tokens;
        this.log = log;
        this.arvoreDerivacao = arvoreDerivacao;
        
        // Inicializa a árvore de derivação
        this.rootNode = new DefaultMutableTreeNode("Programa");
        this.treeModel = new DefaultTreeModel(rootNode);
        this.arvoreDerivacao.setModel(treeModel);
        
        // Inicializa o documento e estilos
        this.doc = log.getStyledDocument();
        this.errorStyle = log.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, java.awt.Color.RED);
    }

    public void parse() {
        insertLog("Iniciando análise sintática...\n", null);
        
        parseProgram();
        
        insertLog("\n\nFim da análise sintática\n\n", null);

        if (!isAtEnd()) {
            insertLog("Tokens inesperados após o fim do programa.\n", errorStyle);
            error("Tokens inesperados após o fim do programa.");
        }
        
        for (int i = 0; i < arvoreDerivacao.getRowCount(); i++) {
            arvoreDerivacao.expandRow(i);
        }
    }

    private DefaultMutableTreeNode parseProgram() {
        DefaultMutableTreeNode programNode = addNode(rootNode, "Programa");
        insertLog("\n-Programa detectado", null);
        
        while (!isAtEnd()) {
            DefaultMutableTreeNode statementNode = parseStatement();
            if (statementNode != null) {
                programNode.add(statementNode);
            }
        }
        
        treeModel.reload(); // Atualiza a visualização da árvore
        
        return programNode;
    }

    private DefaultMutableTreeNode parseStatement() {
        Token first = peek();
        DefaultMutableTreeNode statementNode = addNode(null, "Declaração");

        if (first.type == TokenType.KEYWORD && validTypes.contains(first.value)) {
            DefaultMutableTreeNode declNode = parseDeclaration();
            statementNode.add(declNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");

        } else if (first.type == TokenType.IDENTIFIER) {
            DefaultMutableTreeNode assignNode = parseAssignment();
            statementNode.add(assignNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");

        } else if (first.type == TokenType.KEYWORD && first.value.equals("System.out.println")) {
            DefaultMutableTreeNode printNode = parsePrint();
            statementNode.add(printNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");

        } else if (first.type == TokenType.KEYWORD && first.value.equals("if")) {
            DefaultMutableTreeNode ifNode = parseIf();
            statementNode.add(ifNode);

        } else if (first.type == TokenType.KEYWORD && first.value.equals("while")) {
            DefaultMutableTreeNode whileNode = parseWhile();
            statementNode.add(whileNode);

        } else {
            insertLog("\n----------\nComando inválido iniciado por: " + first.value + "\n", errorStyle);
            error("Esperado declaração, atribuição, impressão, if ou while");
        }

        return statementNode;
    }

    private DefaultMutableTreeNode parsePrint() {
        DefaultMutableTreeNode printNode = addNode(null, "Print");
        insertLog("\n-Comando de impressão detectado", null);
        
        // PRINT
        Token printToken = advance();
        addNode(printNode, "Keyword: " + printToken.value);
        insertLog("\n-Keyword detectada: " + printToken.value, null);

        // "("
        expect(TokenType.SEPARATOR, "(");
        addNode(printNode, "(");
        insertLog("\n-Aberto parêntese '('", null);

        // Expressão dentro do print
        DefaultMutableTreeNode exprNode = parseExpression();
        printNode.add(exprNode);

        // ")"
        expect(TokenType.SEPARATOR, ")");
        addNode(printNode, ")");
        insertLog("\n-Fechado parêntese ')'", null);
        
        return printNode;
    }

    private DefaultMutableTreeNode parseAssignment() {
        DefaultMutableTreeNode assignNode = addNode(null, "Atribuição");
        insertLog("\n-Atribuição detectada", null);

        // IDENTIFICADOR
        Token id = advance();
        addNode(assignNode, "Identificador: " + id.value);
        insertLog("\n-Identificador detectado: " + id.value, null);

        // "="
        expect(TokenType.OPERATOR, "=");
        addNode(assignNode, "=");
        insertLog("\n-Operador '=' detectado", null);

        // Expressão
        DefaultMutableTreeNode exprNode = parseExpression();
        assignNode.add(exprNode);
        
        return assignNode;
    }

    private DefaultMutableTreeNode parseDeclaration() {
        DefaultMutableTreeNode declNode = addNode(null, "Declaração");
        insertLog("\n-Declaração detectada", null);
        
        DefaultMutableTreeNode typeNode = parseType();
        declNode.add(typeNode);
        
        Token identifier = expectWithReturn(TokenType.IDENTIFIER, null);
        addNode(declNode, "Identificador: " + identifier.value);

        if (match(TokenType.OPERATOR, "=")) {
            addNode(declNode, "=");
            insertLog("\n-Encontrou o '=', expressão detectada", null);
            DefaultMutableTreeNode exprNode = parseExpression();
            declNode.add(exprNode);
        }
        
        return declNode;
    }

    private DefaultMutableTreeNode parseType() {
        Token token = advance();
        DefaultMutableTreeNode typeNode = addNode(null, "Tipo: " + token.value);
        
        insertLog("\n-Tipo detectado: " + token.value, null);
        
        if (!(token.type == TokenType.KEYWORD && validTypes.contains(token.value))) {
            insertLog("--------\nERRO!\nTipo inválido: " + token.value + "\n\n", errorStyle);
            error("Esperado tipo primitivo, encontrado: " + token.value);
        }
        
        return typeNode;
    }

    private DefaultMutableTreeNode parseExpression() {
        DefaultMutableTreeNode exprNode = addNode(null, "Expressão");
        insertLog("\n-Expressão detectada", null);
        
        DefaultMutableTreeNode leftNode = parseTerm();
        exprNode.add(leftNode);
        
        // enquanto achar + ou -
        while (true) {
            if (match(TokenType.OPERATOR, "+") || match(TokenType.OPERATOR, "-")) {
                Token op = tokens.get(position - 1);
                DefaultMutableTreeNode opNode = addNode(exprNode, "Operador: " + op.value);
                insertLog("\n-Operador aditivo detectado: " + op.value, null);
                
                DefaultMutableTreeNode rightNode = parseTerm();
                opNode.add(rightNode);
            } else {
                break;
            }
        }
        
        return exprNode;
    }

    private DefaultMutableTreeNode parseTerm() {
        DefaultMutableTreeNode termNode = addNode(null, "Termo");
        
        DefaultMutableTreeNode leftNode = parseFactor();
        termNode.add(leftNode);
        
        // enquanto achar * ou /
        while (true) {
            if (match(TokenType.OPERATOR, "*") || match(TokenType.OPERATOR, "/")) {
                Token op = tokens.get(position - 1);
                DefaultMutableTreeNode opNode = addNode(termNode, "Operador: " + op.value);
                insertLog("\n-Operador multiplicativo detectado: " + op.value, null);
                
                DefaultMutableTreeNode rightNode = parseFactor();
                opNode.add(rightNode);
            } else {
                break;
            }
        }
        
        return termNode;
    }

    private DefaultMutableTreeNode parseFactor() {
        Token tok = peek();
        DefaultMutableTreeNode factorNode = addNode(null, "Fator");

        // número literal
        if (tok.type == TokenType.NUMBER) {
            Token num = advance();
            addNode(factorNode, "Número: " + num.value);
            insertLog("\n-Número detectado: " + num.value, null);
            return factorNode;
        }

        // string literal
        if (tok.type == TokenType.STRING) {
            Token s = advance();
            addNode(factorNode, "String: " + s.value);
            insertLog("\n-String detectada: " + s.value, null);
            return factorNode;
        }

        // identificador
        if (tok.type == TokenType.IDENTIFIER) {
            Token id = advance();
            addNode(factorNode, "Identificador: " + id.value);
            insertLog("\n-Identificador detectado: " + id.value, null);
            return factorNode;
        }

        // boolean literal
        if (tok.type == TokenType.KEYWORD &&
            (tok.value.equals("true") || tok.value.equals("false"))) {
            Token b = advance();
            addNode(factorNode, "Boolean: " + b.value);
            insertLog("\n-Boolean detectado: " + b.value, null);
            return factorNode;
        }

        // subexpressão entre parênteses
        if (match(TokenType.SEPARATOR, "(")) {
            addNode(factorNode, "(");
            insertLog("\n-Abertura de parênteses '('", null);
            
            DefaultMutableTreeNode exprNode = parseExpression();
            factorNode.add(exprNode);
            
            expect(TokenType.SEPARATOR, ")");
            addNode(factorNode, ")");
            insertLog("\n-Fechamento de parênteses ')'", null);
            return factorNode;
        }

        // nenhum caso válido
        insertLog("\nFator inválido: " + tok.value + "\n", errorStyle);
        error("Fator inválido: " + tok.value);
        return factorNode;
    }

    private DefaultMutableTreeNode parseIf() {
        DefaultMutableTreeNode ifNode = addNode(null, "If");
        insertLog("\n-Comando 'if' detectado", null);
        
        advance(); // consome o 'if'
        addNode(ifNode, "if");

        expect(TokenType.SEPARATOR, "(");
        addNode(ifNode, "(");
        
        DefaultMutableTreeNode condNode = parseCondition();
        ifNode.add(condNode);
        
        expect(TokenType.SEPARATOR, ")");
        addNode(ifNode, ")");

        expect(TokenType.SEPARATOR, "{");
        addNode(ifNode, "{");
        
        DefaultMutableTreeNode bodyNode = addNode(ifNode, "Bloco");
        while (!match(TokenType.SEPARATOR, "}")) {
            DefaultMutableTreeNode stmtNode = parseStatement();
            bodyNode.add(stmtNode);
        }
        addNode(ifNode, "}");

        // else opcional
        if (match(TokenType.KEYWORD, "else")) {
            DefaultMutableTreeNode elseNode = addNode(ifNode, "Else");
            addNode(elseNode, "else");
            
            expect(TokenType.SEPARATOR, "{");
            addNode(elseNode, "{");
            
            DefaultMutableTreeNode elseBodyNode = addNode(elseNode, "Bloco");
            while (!match(TokenType.SEPARATOR, "}")) {
                DefaultMutableTreeNode stmtNode = parseStatement();
                elseBodyNode.add(stmtNode);
            }
            addNode(elseNode, "}");
        }
        
        return ifNode;
    }

    private DefaultMutableTreeNode parseWhile() {
        DefaultMutableTreeNode whileNode = addNode(null, "While");
        insertLog("\n-Comando 'while' detectado", null);
        
        advance(); // consome o 'while'
        addNode(whileNode, "while");

        expect(TokenType.SEPARATOR, "(");
        addNode(whileNode, "(");
        
        DefaultMutableTreeNode condNode = parseCondition();
        whileNode.add(condNode);
        
        expect(TokenType.SEPARATOR, ")");
        addNode(whileNode, ")");

        expect(TokenType.SEPARATOR, "{");
        addNode(whileNode, "{");
        
        DefaultMutableTreeNode bodyNode = addNode(whileNode, "Bloco");
        while (!match(TokenType.SEPARATOR, "}")) {
            DefaultMutableTreeNode stmtNode = parseStatement();
            bodyNode.add(stmtNode);
        }
        addNode(whileNode, "}");
        
        return whileNode;
    }

    private DefaultMutableTreeNode parseCondition() {
        DefaultMutableTreeNode condNode = addNode(null, "Condição");
        insertLog("\n-Condição detectada", null);

        // Expressão da esquerda
        DefaultMutableTreeNode leftExpr = parseExpression();
        condNode.add(leftExpr);

        // Operador relacional
        Token opToken = peek();
        boolean isRelOp = opToken.type == TokenType.OPERATOR &&
            (opToken.value.equals("==") || opToken.value.equals("!=") ||
             opToken.value.equals("<")  || opToken.value.equals(">")  ||
             opToken.value.equals("<=") || opToken.value.equals(">="));

        if (!isRelOp) {
            insertLog("\nOperador relacional inválido: " + opToken.value + "\n", errorStyle);
            error("Esperado operador relacional, encontrado: " + opToken.value);
        }

        Token op = advance();
        DefaultMutableTreeNode opNode = addNode(condNode, "Operador: " + op.value);
        insertLog("\n-Operador relacional detectado: " + op.value, null);

        // Expressão da direita
        DefaultMutableTreeNode rightExpr = parseExpression();
        opNode.add(rightExpr);
        
        return condNode;
    }

    // Método auxiliar para adicionar nós à árvore
    private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, String text) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);
        if (parent != null) {
            parent.add(node);
        }
        return node;
    }

    // Método utilitário para expect que retorna o token
    private Token expectWithReturn(TokenType type, String value) {
        insertLog("\n-Esperando token: " + (value != null ? value : type), null);
        
        if (isAtEnd()) {
            error("Fim inesperado dos tokens");
        }
        
        Token token = peek();
        if (token.type != type || (value != null && !token.value.equals(value))) {
            insertLog("\n----------\nToken esperado: " + (value != null ? value : type) + "\n", errorStyle);
            error("Esperado token " + (value != null ? value : type));
        }
        
        advance();
        insertLog("\n-Token " + (value != null ? value : type) + " encontrado", null);
        return token;
    }

    // Utilitários originais (mantidos)
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
        expectWithReturn(type, value); // Reutiliza o método que retorna token
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