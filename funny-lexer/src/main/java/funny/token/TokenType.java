package funny.token;

public enum TokenType {
    EOF, COLON, REQUIRES, ENSURES, USES, TYPE_INT, TRUE, FALSE, NOT, AND, OR, IMPLIES,
    FUNCTION,
    RETURNS,
    WHILE,
    IF,
    ELSE,
    ASSERT,
    LENGTH,
    INVARIANT,
    ASSUME,
    CLASS,

    IDENT,
    INT,

    PLUS,
    MINUS,
    MUL,
    DIV,

    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,
    COMMA,
    SEMICOLON,

    EQ,
    NE,
    LE,
    GE,
    LT,
    GT,
    ASSIGN,

    WS,
    COMMENT
}