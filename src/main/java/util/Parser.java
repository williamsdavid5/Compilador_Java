package util;

import java.util.List;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import util.analisadorLexico.TokenType;

public class Parser {
    private final List<Token> tokens;
    
    private int position = 0;
    private JTextPane log;
    private JTree arvoreDerivacao;
    private JCheckBox passoAPasso;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;
    
    private StyledDocument doc;
    private Style defaultStyle;
    private Style errorStyle;
    private Style highlightStyle;
    
    private int delayMillis = 500; // Delay de 0.5 segundos
    
    // Tipos primitivos aceitos para declaração
    private static final Set<String> validTypes = Set.of("int", "float", "boolean", "String");

    public Parser(List<Token> tokens, JTextPane log, JTree arvoreDerivacao, JCheckBox passoAPasso) {
        this.tokens = tokens;
        this.log = log;
        this.arvoreDerivacao = arvoreDerivacao;
        this.passoAPasso = passoAPasso;
        
        // Inicializa a árvore de derivação
        this.rootNode = new DefaultMutableTreeNode("Programa");
        this.treeModel = new DefaultTreeModel(rootNode);
        this.arvoreDerivacao.setModel(treeModel);
        
        // Inicializa o documento e estilos
        this.doc = log.getStyledDocument();
        this.errorStyle = log.addStyle("error", null);
        StyleConstants.setForeground(errorStyle, java.awt.Color.RED);
        
        try {
            defaultStyle = log.addStyle("default", null);
            StyleConstants.setFontFamily(defaultStyle, "Monospaced");
            highlightStyle = log.addStyle("highlight", null);
            StyleConstants.setBold(highlightStyle, true);
        } catch (Exception e) {
            // ignora
        }
    }

    private void applyDelay() {
        if (passoAPasso != null && passoAPasso.isSelected()) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    private void expandAllTreeNodes() {
        for (int i = 0; i < arvoreDerivacao.getRowCount(); i++) {
            arvoreDerivacao.expandRow(i);
        }
    }
    
    private void ensureTreeVisible() {
        SwingUtilities.invokeLater(() -> {
            expandAllTreeNodes();
            
            // Rola até o final da árvore
            int rowCount = arvoreDerivacao.getRowCount();
            if (rowCount > 0) {
                arvoreDerivacao.scrollRowToVisible(rowCount - 1);
            }
            
            // Força repaint para atualização visual imediata
            arvoreDerivacao.repaint();
        });
    }
    
    public void parse() {
        insertLog("Iniciando análise sintática...\n", null);
        applyDelay();
        parseProgram();
        
        applyDelay();
        insertLog("\n\nFim da análise sintática\n\n", null);
        applyDelay();

        if (!isAtEnd()) {
            insertLog("Tokens inesperados após o fim do programa.\n", errorStyle);
            error("Tokens inesperados após o fim do programa.");
        }
        
        SwingUtilities.invokeLater(() -> {
            expandAllTreeNodes();
        });
    }

    private DefaultMutableTreeNode parseProgram() {
        DefaultMutableTreeNode programNode = addNode(rootNode, "Programa");
        insertLog("\n-Programa detectado", null);
        applyDelay();
        
        while (!isAtEnd()) {
            DefaultMutableTreeNode statementNode = parseStatement();
            if (statementNode != null) {
                attach(programNode, statementNode);
                applyDelay();
            }
        }
        
        applyDelay();
        
        return programNode;
    }

    private DefaultMutableTreeNode parseStatement() {
        Token first = peek();
        DefaultMutableTreeNode statementNode = addNode(null, "Declaração");
        applyDelay();

        if (first.type == TokenType.KEYWORD && validTypes.contains(first.value)) {
            DefaultMutableTreeNode declNode = parseDeclaration();
            attach(statementNode, declNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");
            applyDelay();

        } else if (first.type == TokenType.IDENTIFIER) {
            DefaultMutableTreeNode assignNode = parseAssignment();
            attach(statementNode, assignNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");
            applyDelay();

        } else if (first.type == TokenType.KEYWORD && first.value.equals("System.out.println")) {
            DefaultMutableTreeNode printNode = parsePrint();
            attach(statementNode, printNode);
            expect(TokenType.SEPARATOR, ";");
            addNode(statementNode, ";");
            applyDelay();

        } else if (first.type == TokenType.KEYWORD && first.value.equals("if")) {
            DefaultMutableTreeNode ifNode = parseIf();
            attach(statementNode, ifNode);
            applyDelay();

        } else if (first.type == TokenType.KEYWORD && first.value.equals("while")) {
            DefaultMutableTreeNode whileNode = parseWhile();
            attach(statementNode, whileNode);
            applyDelay();

        } else {
            insertLog("\n----------\nComando inválido iniciado por: " + first.value + "\n", errorStyle);
            error("Esperado declaração, atribuição, impressão, if ou while");
        }

        return statementNode;
    }

    private DefaultMutableTreeNode parsePrint() {
        DefaultMutableTreeNode printNode = addNode(null, "Print");
        insertLog("\n-Comando de impressão detectado", null);
        applyDelay();
        
        // PRINT
        Token printToken = advance();
        addNode(printNode, "Keyword: " + printToken.value);
        insertLog("\n-Keyword detectada: " + printToken.value, null);
        applyDelay();

        // "("
        expect(TokenType.SEPARATOR, "(");
        addNode(printNode, "(");
        insertLog("\n-Aberto parêntese '('", null);
        applyDelay();

        // Expressão dentro do print
        DefaultMutableTreeNode exprNode = parseExpression();
        attach(printNode, exprNode);
        applyDelay();

        // ")"
        expect(TokenType.SEPARATOR, ")");
        addNode(printNode, ")");
        insertLog("\n-Fechado parêntese ')'", null);
        applyDelay();
        
        return printNode;
    }

    private DefaultMutableTreeNode parseAssignment() {
        DefaultMutableTreeNode assignNode = addNode(null, "Atribuição");
        insertLog("\n-Atribuição detectada", null);
        applyDelay();

        // IDENTIFICADOR
        Token id = advance();
        addNode(assignNode, "Identificador: " + id.value);
        insertLog("\n-Identificador detectado: " + id.value, null);
        applyDelay();

        // "="
        expect(TokenType.OPERATOR, "=");
        addNode(assignNode, "=");
        insertLog("\n-Operador '=' detectado", null);
        applyDelay();

        // Expressão
        DefaultMutableTreeNode exprNode = parseExpression();
        attach(assignNode, exprNode);
        applyDelay();
        
        return assignNode;
    }

    private DefaultMutableTreeNode parseDeclaration() {
        DefaultMutableTreeNode declNode = addNode(null, "Declaração");
        insertLog("\n-Declaração detectada", null);
        applyDelay();
        
        DefaultMutableTreeNode typeNode = parseType();
        attach(declNode, typeNode);
        applyDelay();
        
        Token identifier = expectWithReturn(TokenType.IDENTIFIER, null);
        addNode(declNode, "Identificador: " + identifier.value);
        applyDelay();

        if (match(TokenType.OPERATOR, "=")) {
            addNode(declNode, "=");
            insertLog("\n-Encontrou o '=', expressão detectada", null);
            applyDelay();
            DefaultMutableTreeNode exprNode = parseExpression();
            attach(declNode, exprNode);
            applyDelay();
        }
        
        return declNode;
    }

    private DefaultMutableTreeNode parseType() {
        Token token = advance();
        DefaultMutableTreeNode typeNode = addNode(null, "Tipo: " + token.value);
        
        insertLog("\n-Tipo detectado: " + token.value, null);
        applyDelay();
        
        if (!(token.type == TokenType.KEYWORD && validTypes.contains(token.value))) {
            insertLog("--------\nERRO!\nTipo inválido: " + token.value + "\n\n", errorStyle);
            error("Esperado tipo primitivo, encontrado: " + token.value);
        }
        
        return typeNode;
    }

    private DefaultMutableTreeNode parseExpression() {
        DefaultMutableTreeNode exprNode = addNode(null, "Expressão");
        insertLog("\n-Expressão detectada", null);
        applyDelay();
        
        DefaultMutableTreeNode leftNode = parseTerm();
        attach(exprNode, leftNode);
        applyDelay();
        
        // enquanto achar + ou -
        while (true) {
            if (match(TokenType.OPERATOR, "+") || match(TokenType.OPERATOR, "-")) {
                Token op = tokens.get(position - 1);
                DefaultMutableTreeNode opNode = addNode(exprNode, "Operador: " + op.value);
                insertLog("\n-Operador aditivo detectado: " + op.value, null);
                applyDelay();
                
                DefaultMutableTreeNode rightNode = parseTerm();
                attach(opNode, rightNode);
                applyDelay();
            } else {
                break;
            }
        }
        
        return exprNode;
    }

    private DefaultMutableTreeNode parseTerm() {
        DefaultMutableTreeNode termNode = addNode(null, "Termo");
        applyDelay();
        
        DefaultMutableTreeNode leftNode = parseFactor();
        attach(termNode, leftNode);
        applyDelay();
        
        // enquanto achar * ou /
        while (true) {
            if (match(TokenType.OPERATOR, "*") || match(TokenType.OPERATOR, "/")) {
                Token op = tokens.get(position - 1);
                DefaultMutableTreeNode opNode = addNode(termNode, "Operador: " + op.value);
                insertLog("\n-Operador multiplicativo detectado: " + op.value, null);
                applyDelay();
                
                DefaultMutableTreeNode rightNode = parseFactor();
                attach(opNode, rightNode);
                applyDelay();
            } else {
                break;
            }
        }
        
        return termNode;
    }

    private DefaultMutableTreeNode parseFactor() {
        Token tok = peek();
        DefaultMutableTreeNode factorNode = addNode(null, "Fator");
        applyDelay();

        // número literal
        if (tok.type == TokenType.NUMBER) {
            Token num = advance();
            addNode(factorNode, "Número: " + num.value);
            insertLog("\n-Número detectado: " + num.value, null);
            applyDelay();
            return factorNode;
        }

        // string literal
        if (tok.type == TokenType.STRING) {
            Token s = advance();
            addNode(factorNode, "String: " + s.value);
            insertLog("\n-String detectada: " + s.value, null);
            applyDelay();
            return factorNode;
        }

        // identificador
        if (tok.type == TokenType.IDENTIFIER) {
            Token id = advance();
            addNode(factorNode, "Identificador: " + id.value);
            insertLog("\n-Identificador detectado: " + id.value, null);
            applyDelay();
            return factorNode;
        }

        // boolean literal
        if (tok.type == TokenType.KEYWORD &&
            (tok.value.equals("true") || tok.value.equals("false"))) {
            Token b = advance();
            addNode(factorNode, "Boolean: " + b.value);
            insertLog("\n-Boolean detectado: " + b.value, null);
            applyDelay();
            return factorNode;
        }

        // subexpressão entre parênteses
        if (match(TokenType.SEPARATOR, "(")) {
            addNode(factorNode, "(");
            insertLog("\n-Abertura de parênteses '('", null);
            applyDelay();
            
            DefaultMutableTreeNode exprNode = parseExpression();
            attach(factorNode, exprNode);
            applyDelay();
            
            expect(TokenType.SEPARATOR, ")");
            addNode(factorNode, ")");
            insertLog("\n-Fechamento de parênteses ')'", null);
            applyDelay();
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
        applyDelay();
        
        advance(); // consome o 'if'
        addNode(ifNode, "if");
        applyDelay();

        expect(TokenType.SEPARATOR, "(");
        addNode(ifNode, "(");
        applyDelay();
        
        DefaultMutableTreeNode condNode = parseCondition();
        attach(ifNode, condNode);
        applyDelay();
        
        expect(TokenType.SEPARATOR, ")");
        addNode(ifNode, ")");
        applyDelay();

        expect(TokenType.SEPARATOR, "{");
        addNode(ifNode, "{");
        applyDelay();
        
        DefaultMutableTreeNode bodyNode = addNode(ifNode, "Bloco");
        applyDelay();
        while (!match(TokenType.SEPARATOR, "}")) {
            DefaultMutableTreeNode stmtNode = parseStatement();
            attach(bodyNode, stmtNode);
            applyDelay();
        }
        addNode(ifNode, "}");
        applyDelay();

        // else opcional
        if (match(TokenType.KEYWORD, "else")) {
            DefaultMutableTreeNode elseNode = addNode(ifNode, "Else");
            addNode(elseNode, "else");
            applyDelay();
            
            expect(TokenType.SEPARATOR, "{");
            addNode(elseNode, "{");
            applyDelay();
            
            DefaultMutableTreeNode elseBodyNode = addNode(elseNode, "Bloco");
            applyDelay();
            while (!match(TokenType.SEPARATOR, "}")) {
                DefaultMutableTreeNode stmtNode = parseStatement();
                attach(elseBodyNode, stmtNode);
                applyDelay();
            }
            addNode(elseNode, "}");
            applyDelay();
        }
        
        return ifNode;
    }

    private DefaultMutableTreeNode parseWhile() {
        DefaultMutableTreeNode whileNode = addNode(null, "While");
        insertLog("\n-Comando 'while' detectado", null);
        applyDelay();
        
        advance(); // consome o 'while'
        addNode(whileNode, "while");
        applyDelay();

        expect(TokenType.SEPARATOR, "(");
        addNode(whileNode, "(");
        applyDelay();
        
        DefaultMutableTreeNode condNode = parseCondition();
        attach(whileNode, condNode);
        applyDelay();
        
        expect(TokenType.SEPARATOR, ")");
        addNode(whileNode, ")");
        applyDelay();

        expect(TokenType.SEPARATOR, "{");
        addNode(whileNode, "{");
        applyDelay();
        
        DefaultMutableTreeNode bodyNode = addNode(whileNode, "Bloco");
        applyDelay();
        while (!match(TokenType.SEPARATOR, "}")) {
            DefaultMutableTreeNode stmtNode = parseStatement();
            attach(bodyNode, stmtNode);
            applyDelay();
        }
        addNode(whileNode, "}");
        applyDelay();
        
        return whileNode;
    }

    private DefaultMutableTreeNode parseCondition() {
        DefaultMutableTreeNode condNode = addNode(null, "Condição");
        insertLog("\n-Condição detectada", null);
        applyDelay();

        // Expressão da esquerda
        DefaultMutableTreeNode leftExpr = parseExpression();
        attach(condNode, leftExpr);
        applyDelay();

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
        applyDelay();

        // Expressão da direita
        DefaultMutableTreeNode rightExpr = parseExpression();
        attach(opNode, rightExpr);
        applyDelay();
        
        return condNode;
    }

    // Método auxiliar para adicionar nós à árvore (THREAD-SAFE)
    private DefaultMutableTreeNode addNode(DefaultMutableTreeNode parent, String text) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(text);

        if (parent != null) {
            Runnable insertTask = () -> {
                treeModel.insertNodeInto(node, parent, parent.getChildCount());
                if (passoAPasso != null && passoAPasso.isSelected()) {
                    ensureTreeVisible();
                }
            };
            
            if (SwingUtilities.isEventDispatchThread()) {
                insertTask.run();
            } else {
                try {
                    SwingUtilities.invokeAndWait(insertTask);
                } catch (Exception e) {
                    SwingUtilities.invokeLater(insertTask);
                }
            }
        }
        return node;
    }

    // Método para anexar nós de forma segura (THREAD-SAFE)
    private void attach(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        if (parent == null || child == null) return;

        Runnable attachTask = () -> {
            // Remove do pai atual se existir
            if (child.getParent() != null) {
                treeModel.removeNodeFromParent(child);
            }
            // Insere no novo pai
            treeModel.insertNodeInto(child, parent, parent.getChildCount());
            
            if (passoAPasso != null && passoAPasso.isSelected()) {
                ensureTreeVisible();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            attachTask.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(attachTask);
            } catch (Exception e) {
                SwingUtilities.invokeLater(attachTask);
            }
        }
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
        applyDelay();
        return token;
    }

    // Método para inserir log de forma thread-safe
    private void insertLog(String text, Style style) {
        Runnable logTask = () -> {
            try {
                doc.insertString(doc.getLength(), text, style);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        };
        
        if (SwingUtilities.isEventDispatchThread()) {
            logTask.run();
        } else {
            SwingUtilities.invokeLater(logTask);
        }
        
        log.setCaretPosition(log.getDocument().getLength());

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
        expectWithReturn(type, value);
    }

    private void error(String message) {
        throw new RuntimeException("Erro de parsing: " + message);
    }
}