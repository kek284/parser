package funny.parser;

import funny.lexer.SourcePosition;
import funny.lexer.Token;
import funny.token.TokenType;
import java.util.ArrayList;
import java.util.List;
import static funny.token.TokenType.*;
import static funny.parser.Ast.*;

public final class Parser {
    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private int current; //индекс текущего токена

    /** Лексер не добавляет EOF; вызывающий передаёт точную позицию конца файла. */
    public Parser(List<Token> tokens, SourcePosition end) {
        this.tokens = new ArrayList<>(tokens);
        this.tokens.add(new Token(EOF, "", end));
    }

    public ParseResult parse() {
        var functions = new ArrayList<Function>();
        SourcePosition start = peek().position();
        if (at(EOF)) error("Ожидалась хотя бы одна функция");
        while (!at(EOF)) {
            try { functions.add(function()); } //метод разбрирает 1 функцию и возвр узел
            catch (ParseError ignored) {
                // переходим к следующей функции.
                while (!at(EOF) && !at(FUNCTION)) advance();
            }
        }
        return new ParseResult(new Program(functions, start), diagnostics);
    }

    private Function function() {
        Token start = expect(FUNCTION);
        String name = expect(IDENT).value();
        expect(LPAREN);
        List<Variable> parameters = variables(true, RPAREN);
        expect(RPAREN);
        Expression requires = match(REQUIRES) ? expression() : null;
        expect(RETURNS);
        var returns = new ArrayList<Variable>();
        returns.add(variable(true));
        while (match(COMMA)) returns.add(variable(true));
        Expression ensures = match(ENSURES) ? expression() : null;
        var locals = new ArrayList<Variable>();
        if (match(USES)) {
            locals.add(variable(false));
            while (match(COMMA)) locals.add(variable(false));
        }
        return new Function(name, parameters, returns, requires, ensures, locals, block(), start.position());
    }

    private List<Variable> variables(boolean typed, TokenType end) {
        var result = new ArrayList<Variable>();
        if (!at(end)) {
            result.add(variable(typed));
            while (match(COMMA)) result.add(variable(typed));
        }
        return result;
    }

    private Variable variable(boolean typed) {
        Token name = expect(IDENT);
        String type = null;
        if (typed || at(COLON)) {
            expect(COLON); expect(TYPE_INT); type = "int";
            if (match(LBRACKET)) { expect(RBRACKET); type = "int[]"; }
        }
        return new Variable(name.value(), type, name.position());
    }

    private Block block() {
        Token start = expect(LBRACE);
        var statements = new ArrayList<Statement>(); //сюда будут добавляться разобранные операторы блока (x, 42)
        while (!at(RBRACE) && !at(EOF) && !at(FUNCTION)) {
            int before = current;
            try { statements.add(statement()); }
            catch (ParseError ignored) { synchronize(before); } //ошибка не уничтожает весь блок
        }
        expect(RBRACE);
        return new Block(statements, start.position());
    }

    private Statement statement() {
        Token start = peek();
        if (at(LBRACE)) return block();
        if (match(IF)) {
            expect(LPAREN); Expression condition = expression(); expect(RPAREN);
            Block thenBranch = block();
            Block elseBranch = match(ELSE) ? block() : null;
            return new If(condition, thenBranch, elseBranch, start.position());
        }
        if (match(WHILE)) {
            expect(LPAREN); Expression condition = expression(); expect(RPAREN);
            Expression invariant = match(INVARIANT) ? expression() : null;
            return new While(condition, invariant, block(), start.position());
        }
        if (match(ASSERT)) {
            Expression predicate = expression(); expect(SEMICOLON);
            return new Assert(predicate, start.position());
        }
        if (match(ASSUME)) {
            Expression predicate = expression(); expect(SEMICOLON);
            return new Assume(predicate, start.position());
        }
        Token name = expect(IDENT);
        var targets = new ArrayList<String>(); targets.add(name.value());
        var indices = new ArrayList<Expression>();
        while (match(LBRACKET)) { indices.add(expression()); expect(RBRACKET); }
        if (indices.isEmpty()) {
            while (match(COMMA)) targets.add(expect(IDENT).value());
        }
        expect(ASSIGN);
        Expression value = expression();
        if (targets.size() > 1 && !(value instanceof Call)) {
            throw error("Для нескольких результатов ожидался вызов функции");
        }
        expect(SEMICOLON);
        if (!indices.isEmpty()) value = update(new Name(name.value(), name.position()), indices, 0, value);
        return new Assign(targets, value, start.position());
    }

    // a[i][j] = v -> a = ArraySet(a, i, ArraySet(ArrayRead(a, i), j, v)).
    private Expression update(Expression array, List<Expression> indices, int i, Expression value) {
        Expression index = indices.get(i);
        Expression inner = i + 1 == indices.size() ? value
                : update(new ArrayRead(array, index, array.position()), indices, i + 1, value);
        return new ArraySet(array, index, inner, array.position());
    }

    private Expression expression() {
        Expression left = or();
        if (match(IMPLIES)) return new Binary("->", left, expression(), left.position());
        return left;
    }
    private Expression or() { return binary(this::and, OR); }
    private Expression and() { return binary(this::negation, AND); }
    private Expression comparison() {
        Expression left = sum();
        if (at(EQ, NE, LT, LE, GT, GE)) {
            String operator = advance().value();
            return new Binary(operator, left, sum(), left.position());
        }
        return left;
    }
    private Expression sum() { return binary(this::product, PLUS, MINUS); }
   
    private Expression product() { return binary(this::unary, MUL, DIV); }
    private Expression binary(java.util.function.Supplier<Expression> operand, TokenType... operators) {
        Expression left = operand.get();
        while (at(operators)) {
            String operator = advance().value();
            left = new Binary(operator, left, operand.get(), left.position()); //создание левого дерева
        }
        return left;
    }
    private Expression negation() {
        if (at(NOT)) {
            Token operator = advance();
            return new Unary(operator.value(), negation(), operator.position());
        }
        return comparison();
    }
    private Expression unary() {
        if (at(MINUS)) {
            Token operator = advance();
            return new Unary(operator.value(), unary(), operator.position());
        }
        return postfix();
    }
    private Expression postfix() {
        Expression value = primary();
        while (match(LBRACKET)) {
            Expression index = expression(); expect(RBRACKET);
            value = new ArrayRead(value, index, value.position());
        }
        return value;
    }//разбирает базовые выражения
    private Expression primary() {
        Token token = peek();
        if (match(INT)) return new IntegerLiteral(token.value(), token.position());
        if (match(TRUE)) return new BooleanLiteral(true, token.position());
        if (match(FALSE)) return new BooleanLiteral(false, token.position());
        if (match(LPAREN)) {
            Expression value = expression(); expect(RPAREN); return value;//то что внутри скобок
        }
        if (match(IDENT) || match(LENGTH)) {
            if (!match(LPAREN)) {
                if (token.type() == LENGTH) throw error("После length ожидалась '('");
                return new Name(token.value(), token.position());
            }
            var arguments = new ArrayList<Expression>();
            if (!at(RPAREN)) {
                arguments.add(expression());
                while (match(COMMA)) arguments.add(expression());
            }
            expect(RPAREN);
            return new Call(token.value(), arguments, token.position()); //создается узел вызова
        }
        throw error("Ожидалось выражение");
    }

    /** восстанавливаем парсер после ошибки сохраняем следующий корректный оператор. */
    private void synchronize(int before) {
        if (current == before && !at(EOF, RBRACE, FUNCTION)) advance(); //пропускаем токен
        while (!at(EOF, RBRACE, FUNCTION)) {
            if (match(SEMICOLON)) return; //пропускаем токены до ;
            if (at(IDENT, IF, WHILE, ASSERT, ASSUME, LBRACE)) return;
            advance();
        }
    }
    private Token peek() { return tokens.get(current); }
    private boolean at(TokenType... types) { //относится ли токен к одному из переданных типов
        for (TokenType type : types) if (peek().type() == type) return true;
        return false;
    }
    private Token advance() { Token token = peek(); if (!at(EOF)) current++; return token; }
    
    private boolean match(TokenType type) { if (!at(type)) return false; advance(); return true; }
    private Token expect(TokenType type) {
        if (at(type)) return advance();
        throw error("Ожидался " + type);
    }
    private ParseError error(String message) {
        Token token = peek();
        diagnostics.add(new Diagnostic(token.position(), message + "; получен "
                + (token.type() == EOF ? "конец файла" : "'" + token.value() + "' (" + token.type() + ")")));
        return new ParseError();
    }
    private static final class ParseError extends RuntimeException {}
}
