package funny.lexer;

import funny.token.TokenType;

public record Token(TokenType type, String value, SourcePosition position) {
    public Token(TokenType type, String value) {
        this(type, value, new SourcePosition(0, 1, 1));
    }
}
