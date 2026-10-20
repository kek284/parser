package funny.token;

import java.util.List;

public final class TokenSpecs {
    private TokenSpecs() {}
    public static List<TokenSpec> create() {
        return List.of(
                new TokenSpec(TokenType.REQUIRES, "requires", 0, false),
                new TokenSpec(TokenType.ENSURES, "ensures", 0, false),
                new TokenSpec(TokenType.USES, "uses", 0, false),
                new TokenSpec(TokenType.TYPE_INT, "int", 0, false),
                new TokenSpec(TokenType.TRUE, "true", 0, false),
                new TokenSpec(TokenType.FALSE, "false", 0, false),
                new TokenSpec(TokenType.NOT, "not", 0, false),
                new TokenSpec(TokenType.AND, "and", 0, false),
                new TokenSpec(TokenType.OR, "or", 0, false),
                new TokenSpec(TokenType.IMPLIES, "->", 0, false),
                new TokenSpec(TokenType.COLON, ":", 0, false),
                new TokenSpec(TokenType.FUNCTION, "function", 0, false),
                new TokenSpec(TokenType.RETURNS, "returns", 1, false),
                new TokenSpec(TokenType.WHILE, "while", 3, false),
                new TokenSpec(TokenType.IF, "if", 4, false),
                new TokenSpec(TokenType.ELSE, "else", 5, false),
                new TokenSpec(TokenType.ASSERT, "assert", 6, false),
                new TokenSpec(TokenType.ASSUME, "assume", 7, false),
                new TokenSpec(TokenType.INVARIANT, "invariant", 8, false),
                new TokenSpec(TokenType.LENGTH, "length", 9, false),

                new TokenSpec(TokenType.IDENT, "[A-Za-z_][A-Za-z0-9_]*", 20, false),
                new TokenSpec(TokenType.INT, "0|[1-9][0-9]*", 21, false),

                new TokenSpec(TokenType.WS, "[ \\t\\r\\n]+", 30, true),
                new TokenSpec(TokenType.COMMENT, "//[^\\r\\n]*", 31, true),

                new TokenSpec(TokenType.LPAREN,   "\\(", 40, false),
                new TokenSpec(TokenType.RPAREN,   "\\)", 41, false),
                new TokenSpec(TokenType.LBRACKET, "\\[", 42, false),
                new TokenSpec(TokenType.RBRACKET, "\\]", 43, false),
                new TokenSpec(TokenType.LBRACE,   "\\{", 44, false),
                new TokenSpec(TokenType.RBRACE,   "\\}", 45, false),
                new TokenSpec(TokenType.COMMA,    ",",   46, false),
                new TokenSpec(TokenType.SEMICOLON,";",   47, false),

                new TokenSpec(TokenType.EQ, "==", 50, false),  // ==
                new TokenSpec(TokenType.NE, "!=", 51, false),  // !=
                new TokenSpec(TokenType.LE, "<=", 52, false),  // <=
                new TokenSpec(TokenType.GE, ">=", 53, false),  // >=

                new TokenSpec(TokenType.LT,     "<", 54, false),
                new TokenSpec(TokenType.GT,     ">", 55, false),
                new TokenSpec(TokenType.ASSIGN, "=", 56, false),

                new TokenSpec(TokenType.PLUS,  "\\+", 57, false),  // + экранируется
                new TokenSpec(TokenType.MINUS, "-",   58, false),  // - не метасимвол
                new TokenSpec(TokenType.MUL,   "\\*", 59, false),  // * экранируется
                new TokenSpec(TokenType.DIV,   "/",   60, false)   // / не метасимвол

        );
    }
}
