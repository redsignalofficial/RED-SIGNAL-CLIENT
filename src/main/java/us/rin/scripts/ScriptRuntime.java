package us.rin.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ScriptRuntime {

    private ScriptRuntime() {
    }

    public static void run(String code, BooleanSupplier isRunning, Runnable requestStop, Consumer<String> log) {
        if (code == null || code.trim().isEmpty()) {
            if (log != null) log.accept("Script is empty.");
            return;
        }
        new ScriptEngine(code, isRunning, requestStop, log).run();
    }

    interface Fn {
        ScriptEngine.Value invoke(List<ScriptEngine.Value> args, SourcePos pos);
    }

    record SourcePos(int line, int col) {
    }

    public static final class ScriptError extends RuntimeException {
        public final int line;
        public final int col;

        ScriptError(SourcePos pos, String message) {
            super(message);
            this.line = pos == null ? 1 : pos.line;
            this.col = pos == null ? 1 : pos.col;
        }
    }

    enum TokenKind {
        IDENT,
        NUMBER,
        STRING,
        TRUE,
        FALSE,
        NULL,
        FUNC,
        RETURN,
        TRY,
        CATCH,
        LET,
        CONST,
        START,
        STOP,
        LOOP,
        IF,
        ELSE,
        BREAK,
        CONTINUE,
        PLUS,
        MINUS,
        STAR,
        SLASH,
        PERCENT,
        EQ,
        EQEQ,
        NEQ,
        LT,
        LTE,
        GT,
        GTE,
        AND_AND,
        OR_OR,
        BANG,
        DOT,
        COMMA,
        COLON,
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        LBRACE,
        RBRACE,
        SEMI,
        EOF
    }

    private record Token(TokenKind kind, String text, SourcePos pos) {
    }

    static final class Lexer {
        private final String src;
        private int i = 0;
        private int line = 1;
        private int col = 1;

        Lexer(String src) {
            this.src = src == null ? "" : src;
        }

        Token next() {
            skipWsAndComments();
            if (i >= src.length()) return new Token(TokenKind.EOF, "", new SourcePos(line, col));

            SourcePos p = new SourcePos(line, col);
            char c = src.charAt(i);

            if (c == ';') {
                advance();
                return new Token(TokenKind.SEMI, ";", p);
            }
            if (c == '+') {
                advance();
                return new Token(TokenKind.PLUS, "+", p);
            }
            if (c == '-') {
                advance();
                return new Token(TokenKind.MINUS, "-", p);
            }
            if (c == '*') {
                advance();
                return new Token(TokenKind.STAR, "*", p);
            }
            if (c == '/') {
                advance();
                return new Token(TokenKind.SLASH, "/", p);
            }
            if (c == '%') {
                advance();
                return new Token(TokenKind.PERCENT, "%", p);
            }
            if (c == '=') {
                advance();
                if (i < src.length() && src.charAt(i) == '=') {
                    advance();
                    return new Token(TokenKind.EQEQ, "==", p);
                }
                return new Token(TokenKind.EQ, "=", p);
            }
            if (c == '!') {
                advance();
                if (i < src.length() && src.charAt(i) == '=') {
                    advance();
                    return new Token(TokenKind.NEQ, "!=", p);
                }
                return new Token(TokenKind.BANG, "!", p);
            }
            if (c == '<') {
                advance();
                if (i < src.length() && src.charAt(i) == '=') {
                    advance();
                    return new Token(TokenKind.LTE, "<=", p);
                }
                return new Token(TokenKind.LT, "<", p);
            }
            if (c == '>') {
                advance();
                if (i < src.length() && src.charAt(i) == '=') {
                    advance();
                    return new Token(TokenKind.GTE, ">=", p);
                }
                return new Token(TokenKind.GT, ">", p);
            }
            if (c == '&') {
                advance();
                if (i < src.length() && src.charAt(i) == '&') {
                    advance();
                    return new Token(TokenKind.AND_AND, "&&", p);
                }
                throw new ScriptError(p, "Unexpected character: &");
            }
            if (c == '|') {
                advance();
                if (i < src.length() && src.charAt(i) == '|') {
                    advance();
                    return new Token(TokenKind.OR_OR, "||", p);
                }
                throw new ScriptError(p, "Unexpected character: |");
            }
            if (c == '.') {
                advance();
                return new Token(TokenKind.DOT, ".", p);
            }
            if (c == ',') {
                advance();
                return new Token(TokenKind.COMMA, ",", p);
            }
            if (c == ':') {
                advance();
                return new Token(TokenKind.COLON, ":", p);
            }
            if (c == '(') {
                advance();
                return new Token(TokenKind.LPAREN, "(", p);
            }
            if (c == ')') {
                advance();
                return new Token(TokenKind.RPAREN, ")", p);
            }
            if (c == '[') {
                advance();
                return new Token(TokenKind.LBRACKET, "[", p);
            }
            if (c == ']') {
                advance();
                return new Token(TokenKind.RBRACKET, "]", p);
            }
            if (c == '{') {
                advance();
                return new Token(TokenKind.LBRACE, "{", p);
            }
            if (c == '}') {
                advance();
                return new Token(TokenKind.RBRACE, "}", p);
            }

            if (c == '"' || c == '\'') {
                return readString();
            }

            if (Character.isDigit(c)) {
                return readNumber();
            }

            if (Character.isLetter(c) || c == '_') {
                return readIdentOrKeyword();
            }

            throw new ScriptError(p, "Unexpected character: " + c);
        }

        private void skipWsAndComments() {
            while (i < src.length()) {
                char c = src.charAt(i);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                    advance();
                    continue;
                }
                if (c == '#') {
                    while (i < src.length() && src.charAt(i) != '\n') advance();
                    continue;
                }
                if (c == '/' && i + 1 < src.length() && src.charAt(i + 1) == '/') {
                    while (i < src.length() && src.charAt(i) != '\n') advance();
                    continue;
                }
                break;
            }
        }

        private Token readString() {
            SourcePos p = new SourcePos(line, col);
            char quote = src.charAt(i);
            advance();
            StringBuilder sb = new StringBuilder();
            boolean esc = false;
            while (i < src.length()) {
                char c = src.charAt(i);
                if (esc) {
                    if (c == 'n') sb.append('\n');
                    else if (c == 't') sb.append('\t');
                    else sb.append(c);
                    esc = false;
                    advance();
                    continue;
                }
                if (c == '\\') {
                    esc = true;
                    advance();
                    continue;
                }
                if (c == quote) {
                    advance();
                    return new Token(TokenKind.STRING, sb.toString(), p);
                }
                sb.append(c);
                advance();
            }
            throw new ScriptError(p, "Unterminated string");
        }

        private Token readNumber() {
            SourcePos p = new SourcePos(line, col);
            int start = i;
            boolean dot = false;
            while (i < src.length()) {
                char c = src.charAt(i);
                if (c == '.' && !dot) {
                    dot = true;
                    advance();
                    continue;
                }
                if (!Character.isDigit(c)) break;
                advance();
            }
            String s = src.substring(start, i);
            return new Token(TokenKind.NUMBER, s, p);
        }

        private Token readIdentOrKeyword() {
            SourcePos p = new SourcePos(line, col);
            int start = i;
            while (i < src.length()) {
                char c = src.charAt(i);
                if (!Character.isLetterOrDigit(c) && c != '_') break;
                advance();
            }
            String s = src.substring(start, i);
            String sl = s.toLowerCase();
            return switch (sl) {
                case "true" -> new Token(TokenKind.TRUE, s, p);
                case "false" -> new Token(TokenKind.FALSE, s, p);
                case "null" -> new Token(TokenKind.NULL, s, p);
                case "func" -> new Token(TokenKind.FUNC, s, p);
                case "return" -> new Token(TokenKind.RETURN, s, p);
                case "try" -> new Token(TokenKind.TRY, s, p);
                case "catch" -> new Token(TokenKind.CATCH, s, p);
                case "let" -> new Token(TokenKind.LET, s, p);
                case "const" -> new Token(TokenKind.CONST, s, p);
                case "start" -> new Token(TokenKind.START, s, p);
                case "stop" -> new Token(TokenKind.STOP, s, p);
                case "loop" -> new Token(TokenKind.LOOP, s, p);
                case "if" -> new Token(TokenKind.IF, s, p);
                case "else" -> new Token(TokenKind.ELSE, s, p);
                case "break" -> new Token(TokenKind.BREAK, s, p);
                case "continue" -> new Token(TokenKind.CONTINUE, s, p);
                default -> new Token(TokenKind.IDENT, s, p);
            };
        }

        private void advance() {
            if (i >= src.length()) return;
            char c = src.charAt(i);
            i++;
            if (c == '\n') {
                line++;
                col = 1;
            } else {
                col++;
            }
        }
    }

    static final class Parser {
        private final Lexer lexer;
        private Token current;
        private Token next;

        Parser(Lexer lexer) {
            this.lexer = lexer;
            this.current = lexer.next();
            this.next = lexer.next();
        }

        boolean isAtEnd() {
            return current.kind == TokenKind.EOF;
        }

        boolean consumeOptional(TokenKind kind) {
            if (current.kind == kind) {
                advance();
                return true;
            }
            return false;
        }

        Stmt parseStatement() {
            if (current.kind == TokenKind.EOF) return null;
            if (current.kind == TokenKind.SEMI) {
                Token semi = current;
                advance();
                return new ExprStmt(new LiteralExpr(ScriptEngine.Value.nullValue(), semi.pos));
            }

            if (current.kind == TokenKind.FUNC) {
                SourcePos p = current.pos;
                advance();
                Token name = consume(TokenKind.IDENT, "Expected function name");
                consume(TokenKind.LPAREN, "Expected '('");
                List<String> params = new ArrayList<>();
                if (!consumeOptional(TokenKind.RPAREN)) {
                    do {
                        Token param = consume(TokenKind.IDENT, "Expected parameter name");
                        params.add(param.text);
                    } while (consumeOptional(TokenKind.COMMA));
                    consume(TokenKind.RPAREN, "Expected ')'");
                }
                List<Stmt> body = parseBlock("Unterminated func block");
                return new FuncDeclStmt(name.text, params, body, p);
            }

            if (current.kind == TokenKind.RETURN) {
                SourcePos p = current.pos;
                advance();
                Expr expr = null;
                if (current.kind != TokenKind.SEMI && current.kind != TokenKind.RBRACE) {
                    expr = parseExpr();
                }
                return new ReturnStmt(expr, p);
            }

            if (current.kind == TokenKind.TRY) {
                SourcePos p = current.pos;
                advance();
                List<Stmt> tryBody = parseBlock("Unterminated try block");
                consume(TokenKind.CATCH, "Expected 'catch'");
                consume(TokenKind.LPAREN, "Expected '('");
                Token name = consume(TokenKind.IDENT, "Expected catch identifier");
                consume(TokenKind.RPAREN, "Expected ')'");
                List<Stmt> catchBody = parseBlock("Unterminated catch block");
                return new TryStmt(tryBody, name.text, catchBody, p);
            }

            if (current.kind == TokenKind.START) {
                Token t = current;
                advance();
                return new StartStmt(t.pos);
            }
            if (current.kind == TokenKind.STOP) {
                Token t = current;
                advance();
                return new StopStmt(t.pos);
            }

            if (current.kind == TokenKind.LOOP) {
                SourcePos p = current.pos;
                advance();
                consume(TokenKind.LPAREN, "Expected '('");
                Token var = consume(TokenKind.IDENT, "Expected loop variable");
                consume(TokenKind.COMMA, "Expected ','");
                Expr count = parseExpr();
                consume(TokenKind.RPAREN, "Expected ')'");
                List<Stmt> body = parseBlock("Unterminated loop block");
                return new LoopStmt(var.text, count, body, p);
            }

            if (current.kind == TokenKind.IF) {
                return parseIf();
            }

            if (current.kind == TokenKind.BREAK) {
                SourcePos p = current.pos;
                advance();
                return new BreakStmt(p);
            }

            if (current.kind == TokenKind.CONTINUE) {
                SourcePos p = current.pos;
                advance();
                return new ContinueStmt(p);
            }

            if (current.kind == TokenKind.LET || current.kind == TokenKind.CONST) {
                boolean isConst = current.kind == TokenKind.CONST;
                SourcePos p = current.pos;
                advance();
                Token name = consume(TokenKind.IDENT, "Expected identifier");
                consume(TokenKind.EQ, "Expected '='");
                Expr expr = parseExpr();
                return new LetStmt(name.text, expr, isConst, p);
            }

            if (current.kind == TokenKind.IDENT && next.kind == TokenKind.EQ) {
                Token name = current;
                advance();
                consume(TokenKind.EQ, "Expected '='");
                Expr expr = parseExpr();
                return new AssignStmt(name.text, expr, name.pos);
            }

            Expr e = parseExpr();
            return new ExprStmt(e);
        }

        private Expr parseExpr() {
            return parseOr();
        }

        private Expr parseOr() {
            Expr left = parseAnd();
            while (current.kind == TokenKind.OR_OR) {
                Token op = current;
                advance();
                Expr right = parseAnd();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseAnd() {
            Expr left = parseEquality();
            while (current.kind == TokenKind.AND_AND) {
                Token op = current;
                advance();
                Expr right = parseEquality();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseEquality() {
            Expr left = parseCompare();
            while (current.kind == TokenKind.EQEQ || current.kind == TokenKind.NEQ) {
                Token op = current;
                advance();
                Expr right = parseCompare();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseCompare() {
            Expr left = parseAdd();
            while (current.kind == TokenKind.LT
                    || current.kind == TokenKind.LTE
                    || current.kind == TokenKind.GT
                    || current.kind == TokenKind.GTE) {
                Token op = current;
                advance();
                Expr right = parseAdd();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseAdd() {
            Expr left = parseMul();
            while (current.kind == TokenKind.PLUS || current.kind == TokenKind.MINUS) {
                Token op = current;
                advance();
                Expr right = parseMul();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseMul() {
            Expr left = parseUnary();
            while (current.kind == TokenKind.PERCENT || current.kind == TokenKind.STAR || current.kind == TokenKind.SLASH) {
                Token op = current;
                advance();
                Expr right = parseUnary();
                left = new BinaryExpr(left, op.kind, right, op.pos);
            }
            return left;
        }

        private Expr parseUnary() {
            if (current.kind == TokenKind.BANG) {
                Token op = current;
                advance();
                Expr right = parseUnary();
                return new UnaryExpr(op.kind, right, op.pos);
            }
            if (current.kind == TokenKind.MINUS) {
                Token op = current;
                advance();
                Expr right = parseUnary();
                return new UnaryExpr(op.kind, right, op.pos);
            }
            return parseChain();
        }

        private Expr parseChain() {
            Expr expr = parsePrimary();
            while (true) {
                if (consumeOptional(TokenKind.DOT)) {
                    Token member = consume(TokenKind.IDENT, "Expected member name");
                    expr = new GetMemberExpr(expr, member.text, member.pos);
                    continue;
                }
                if (current.kind == TokenKind.LPAREN) {
                    SourcePos p = current.pos;
                    advance();
                    List<Expr> args = new ArrayList<>();
                    if (!consumeOptional(TokenKind.RPAREN)) {
                        do {
                            args.add(parseExpr());
                        } while (consumeOptional(TokenKind.COMMA));
                        consume(TokenKind.RPAREN, "Expected ')'");
                    }
                    expr = new CallExpr(expr, args, p);
                    continue;
                }
                break;
            }
            return expr;
        }

        private IfStmt parseIf() {
            SourcePos p = current.pos;
            consume(TokenKind.IF, "Expected 'if'");
            consume(TokenKind.LPAREN, "Expected '('");
            Expr cond = parseExpr();
            consume(TokenKind.RPAREN, "Expected ')'");
            List<Stmt> thenBody = parseBlock("Unterminated if block");

            Stmt elseBranch = null;
            if (current.kind == TokenKind.ELSE) {
                Token elseTok = current;
                advance();
                if (current.kind == TokenKind.IF) {
                    elseBranch = parseIf();
                } else {
                    List<Stmt> elseBody = parseBlock("Unterminated else block");
                    elseBranch = new BlockStmt(elseBody, elseTok.pos);
                }
            }

            return new IfStmt(cond, thenBody, elseBranch, p);
        }

        private List<Stmt> parseBlock(String unterminatedMsg) {
            consume(TokenKind.LBRACE, "Expected '{'");
            List<Stmt> body = new ArrayList<>();
            while (current.kind != TokenKind.RBRACE) {
                if (current.kind == TokenKind.EOF) throw new ScriptError(current.pos, unterminatedMsg);
                Stmt s = parseStatement();
                if (s == null) break;
                body.add(s);
                consumeOptional(TokenKind.SEMI);
            }
            consume(TokenKind.RBRACE, "Expected '}'");
            return body;
        }

        private Expr parsePrimary() {
            Token t = current;
            switch (t.kind) {
                case TRUE -> {
                    advance();
                    return new LiteralExpr(ScriptEngine.Value.bool(true), t.pos);
                }
                case FALSE -> {
                    advance();
                    return new LiteralExpr(ScriptEngine.Value.bool(false), t.pos);
                }
                case NULL -> {
                    advance();
                    return new LiteralExpr(ScriptEngine.Value.nullValue(), t.pos);
                }
                case NUMBER -> {
                    advance();
                    try {
                        return new LiteralExpr(ScriptEngine.Value.number(Double.parseDouble(t.text)), t.pos);
                    } catch (Exception e) {
                        throw new ScriptError(t.pos, "Bad number");
                    }
                }
                case STRING -> {
                    advance();
                    return new LiteralExpr(ScriptEngine.Value.string(t.text), t.pos);
                }
                case IDENT -> {
                    advance();
                    return new VarExpr(t.text, t.pos);
                }
                case LPAREN -> {
                    advance();
                    Expr e = parseExpr();
                    consume(TokenKind.RPAREN, "Expected ')'");
                    return e;
                }
                case LBRACKET -> {
                    SourcePos p = t.pos;
                    advance();
                    List<Expr> items = new ArrayList<>();
                    if (!consumeOptional(TokenKind.RBRACKET)) {
                        do {
                            items.add(parseExpr());
                        } while (consumeOptional(TokenKind.COMMA));
                        consume(TokenKind.RBRACKET, "Expected ']'");
                    }
                    return new ListLiteralExpr(items, p);
                }
                case LBRACE -> {
                    SourcePos p = t.pos;
                    advance();
                    List<MapEntry> entries = new ArrayList<>();
                    if (!consumeOptional(TokenKind.RBRACE)) {
                        do {
                            Expr key;
                            if (current.kind == TokenKind.IDENT) {
                                Token kt = current;
                                advance();
                                key = new LiteralExpr(ScriptEngine.Value.string(kt.text), kt.pos);
                            } else {
                                key = parseExpr();
                            }
                            consume(TokenKind.COLON, "Expected ':'");
                            Expr value = parseExpr();
                            entries.add(new MapEntry(key, value));
                        } while (consumeOptional(TokenKind.COMMA));
                        consume(TokenKind.RBRACE, "Expected '}'");
                    }
                    return new MapLiteralExpr(entries, p);
                }
                default -> throw new ScriptError(t.pos, "Unexpected token: " + t.kind);
            }
        }

        private Token consume(TokenKind kind, String msg) {
            if (current.kind != kind) throw new ScriptError(current.pos, msg);
            Token t = current;
            advance();
            return t;
        }

        private void advance() {
            current = next;
            next = lexer.next();
        }
    }

    private static abstract class Node {
        final SourcePos pos;

        Node(SourcePos pos) {
            this.pos = pos == null ? new SourcePos(1, 1) : pos;
        }
    }

    static abstract class Stmt extends Node {
        Stmt(SourcePos pos) {
            super(pos);
        }
    }

    static final class StartStmt extends Stmt {
        StartStmt(SourcePos pos) {
            super(pos);
        }
    }

    static final class StopStmt extends Stmt {
        StopStmt(SourcePos pos) {
            super(pos);
        }
    }

    static final class LoopStmt extends Stmt {
        final String varName;
        final Expr count;
        final List<Stmt> body;

        LoopStmt(String varName, Expr count, List<Stmt> body, SourcePos pos) {
            super(pos);
            this.varName = varName;
            this.count = count;
            this.body = body == null ? new ArrayList<>() : body;
        }
    }

    static final class LetStmt extends Stmt {
        final String name;
        final Expr expr;
        final boolean isConst;

        LetStmt(String name, Expr expr, boolean isConst, SourcePos pos) {
            super(pos);
            this.name = name;
            this.expr = expr;
            this.isConst = isConst;
        }
    }

    static final class ExprStmt extends Stmt {
        final Expr expr;

        ExprStmt(Expr expr) {
            super(expr == null ? new SourcePos(1, 1) : expr.pos);
            this.expr = expr;
        }
    }

    static final class AssignStmt extends Stmt {
        final String name;
        final Expr expr;

        AssignStmt(String name, Expr expr, SourcePos pos) {
            super(pos);
            this.name = name;
            this.expr = expr;
        }
    }

    static final class BreakStmt extends Stmt {
        BreakStmt(SourcePos pos) {
            super(pos);
        }
    }

    static final class ContinueStmt extends Stmt {
        ContinueStmt(SourcePos pos) {
            super(pos);
        }
    }

    static final class BlockStmt extends Stmt {
        final List<Stmt> body;

        BlockStmt(List<Stmt> body, SourcePos pos) {
            super(pos);
            this.body = body == null ? new ArrayList<>() : body;
        }
    }

    static final class IfStmt extends Stmt {
        final Expr condition;
        final List<Stmt> thenBody;
        final Stmt elseBranch;

        IfStmt(Expr condition, List<Stmt> thenBody, Stmt elseBranch, SourcePos pos) {
            super(pos);
            this.condition = condition;
            this.thenBody = thenBody == null ? new ArrayList<>() : thenBody;
            this.elseBranch = elseBranch;
        }
    }

    static abstract class Expr extends Node {
        Expr(SourcePos pos) {
            super(pos);
        }
    }

    static final class LiteralExpr extends Expr {
        final ScriptEngine.Value value;

        LiteralExpr(ScriptEngine.Value value, SourcePos pos) {
            super(pos);
            this.value = value;
        }
    }

    static final class VarExpr extends Expr {
        final String name;

        VarExpr(String name, SourcePos pos) {
            super(pos);
            this.name = name;
        }
    }

    static final class BinaryExpr extends Expr {
        final Expr left;
        final TokenKind op;
        final Expr right;

        BinaryExpr(Expr left, TokenKind op, Expr right, SourcePos pos) {
            super(pos);
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }

    static final class GetMemberExpr extends Expr {
        final Expr target;
        final String member;

        GetMemberExpr(Expr target, String member, SourcePos pos) {
            super(pos);
            this.target = target;
            this.member = member;
        }
    }

    static final class CallExpr extends Expr {
        final Expr callee;
        final List<Expr> args;

        CallExpr(Expr callee, List<Expr> args, SourcePos pos) {
            super(pos);
            this.callee = callee;
            this.args = args == null ? new ArrayList<>() : args;
        }
    }

    static final class UnaryExpr extends Expr {
        final TokenKind op;
        final Expr right;

        UnaryExpr(TokenKind op, Expr right, SourcePos pos) {
            super(pos);
            this.op = op;
            this.right = right;
        }
    }

    static final class FuncDeclStmt extends Stmt {
        final String name;
        final List<String> params;
        final List<Stmt> body;

        FuncDeclStmt(String name, List<String> params, List<Stmt> body, SourcePos pos) {
            super(pos);
            this.name = name;
            this.params = params == null ? new ArrayList<>() : params;
            this.body = body == null ? new ArrayList<>() : body;
        }
    }

    static final class ReturnStmt extends Stmt {
        final Expr expr;

        ReturnStmt(Expr expr, SourcePos pos) {
            super(pos);
            this.expr = expr;
        }
    }

    static final class TryStmt extends Stmt {
        final List<Stmt> tryBody;
        final String catchName;
        final List<Stmt> catchBody;

        TryStmt(List<Stmt> tryBody, String catchName, List<Stmt> catchBody, SourcePos pos) {
            super(pos);
            this.tryBody = tryBody == null ? new ArrayList<>() : tryBody;
            this.catchName = catchName;
            this.catchBody = catchBody == null ? new ArrayList<>() : catchBody;
        }
    }

    record MapEntry(Expr key, Expr value) {
    }

    static final class ListLiteralExpr extends Expr {
        final List<Expr> items;

        ListLiteralExpr(List<Expr> items, SourcePos pos) {
            super(pos);
            this.items = items == null ? new ArrayList<>() : items;
        }
    }

    static final class MapLiteralExpr extends Expr {
        final List<MapEntry> entries;

        MapLiteralExpr(List<MapEntry> entries, SourcePos pos) {
            super(pos);
            this.entries = entries == null ? new ArrayList<>() : entries;
        }
    }
}
