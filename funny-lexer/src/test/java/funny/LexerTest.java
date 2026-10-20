package funny;

import funny.dfa.Dfa;
import funny.dfa.DfaBuilder;
import funny.dfa.DfaJsonSerializer;
import funny.dfa.DfaMinimizer;
import funny.lexer.Lexer;
import funny.lexer.LexerException;
import funny.lexer.Token;
import funny.nfa.Nfa;
import funny.nfa.NfaBuilder;
import funny.token.TokenSpecs;
import funny.token.TokenType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LexerTest {

    private final Lexer lexer = createLexer();

    @Test
    void acceptsEmptyInput() {
        assertEquals(List.of(), tokenize(""));
    }

    @Test
    void skipsWhitespaceIncludingCrLf() {
        assertEquals(List.of(), tokenize(" \t\r\n"));
    }

    @Test
    void recognizesIdentifiersAndIntegers() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "_value2"),
                        new Token(TokenType.INT, "0"),
                        new Token(TokenType.INT, "42")
                ),
                tokenize("_value2 0 42")
        );
    }

    @Test
    void rejectsIntegersWithLeadingZero() {
        assertThrows(LexerException.class, () -> tokenize("00"));
        assertThrows(LexerException.class, () -> tokenize("01"));
    }

    @Test
    void recognizesAllKeywords() {
        assertEquals(
                List.of(
                        new Token(TokenType.FUNCTION, "function"),
                        new Token(TokenType.RETURNS, "returns"),
                        new Token(TokenType.WHILE, "while"),
                        new Token(TokenType.IF, "if"),
                        new Token(TokenType.ELSE, "else"),
                        new Token(TokenType.ASSERT, "assert"),
                        new Token(TokenType.ASSUME, "assume"),
                        new Token(TokenType.INVARIANT, "invariant"),
                        new Token(TokenType.LENGTH, "length")
                ),
                tokenize(
                        "function returns while if else assert assume invariant length"
                )
        );
    }

    @Test
    void recognizesDelimitersAndOperators() {
        assertEquals(
                List.of(
                        new Token(TokenType.LPAREN, "("),
                        new Token(TokenType.RPAREN, ")"),
                        new Token(TokenType.LBRACKET, "["),
                        new Token(TokenType.RBRACKET, "]"),
                        new Token(TokenType.LBRACE, "{"),
                        new Token(TokenType.RBRACE, "}"),
                        new Token(TokenType.COMMA, ","),
                        new Token(TokenType.SEMICOLON, ";"),
                        new Token(TokenType.PLUS, "+"),
                        new Token(TokenType.MINUS, "-"),
                        new Token(TokenType.MUL, "*"),
                        new Token(TokenType.DIV, "/"),
                        new Token(TokenType.EQ, "=="),
                        new Token(TokenType.NE, "!="),
                        new Token(TokenType.LE, "<="),
                        new Token(TokenType.GE, ">="),
                        new Token(TokenType.LT, "<"),
                        new Token(TokenType.GT, ">"),
                        new Token(TokenType.ASSIGN, "=")
                ),
                tokenize("()[]{},;+-*/==!=<= >= < > =")
        );
    }

    @Test
    void skipsCommentUntilEndOfLine() {
        assertEquals(
                List.of(new Token(TokenType.IDENT, "value")),
                tokenize("// ignored\r\nvalue")
        );
    }

    @Test
    void rejectsNonAsciiCharacterThroughTrap() {
        assertThrows(LexerException.class, () -> tokenize("valueé"));
    }

    @Test
    void serializesCompleteMinimizedDfa() {
        Dfa dfa = createDfa();
        String json = DfaJsonSerializer.serialize(dfa);

        assertTrue(json.contains("\"alphabet\": \"ASCII\""));
        assertTrue(json.contains("\"startState\": " + dfa.start().getId()));
        assertTrue(json.contains("\"trapState\": " + dfa.trap().getId()));
        assertTrue(json.contains("\"token\": \"FUNCTION\""));
        assertTrue(json.contains("\"symbol\": 127"));
        assertTrue(json.contains("\"token\": null"));
    }

    private List<Token> tokenize(String source) {
        return lexer.tokenize(source).stream()
                .map(token -> new Token(token.type(), token.value())).toList();
    }

    private static Lexer createLexer() {
        return new Lexer(createDfa());
    }

    private static Dfa createDfa() {
        Nfa nfa = new NfaBuilder().buildAll(TokenSpecs.create());
        Dfa dfa = new DfaBuilder().build(nfa);
        return new DfaMinimizer().minimize(dfa);
    }

    @Test
    void recognizesCommaBetweenIdentifiers() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "a"),
                        new Token(TokenType.COMMA, ","),
                        new Token(TokenType.IDENT, "b")
                ),
                tokenize("a,b")
        );
    }

    @Test
    void recognizesEmptyParentheses() {
        assertEquals(
                List.of(
                        new Token(TokenType.LPAREN, "("),
                        new Token(TokenType.RPAREN, ")")
                ),
                tokenize("()")
        );
    }

    @Test
    void recognizesEmptyBraces() {
        assertEquals(
                List.of(
                        new Token(TokenType.LBRACE, "{"),
                        new Token(TokenType.RBRACE, "}")
                ),
                tokenize("{}")
        );
    }

    @Test
    void recognizesEmptyBrackets() {
        assertEquals(
                List.of(
                        new Token(TokenType.LBRACKET, "["),
                        new Token(TokenType.RBRACKET, "]")
                ),
                tokenize("[]")
        );
    }

    @Test
    void recognizesSimpleAddition() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "a"),
                        new Token(TokenType.PLUS, "+"),
                        new Token(TokenType.IDENT, "b")
                ),
                tokenize("a+b")
        );
    }

    @Test
    void recognizesSimpleSubtraction() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "a"),
                        new Token(TokenType.MINUS, "-"),
                        new Token(TokenType.INT, "1")
                ),
                tokenize("a-1")
        );
    }

    @Test
    void recognizesSimpleMultiplication() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "x"),
                        new Token(TokenType.MUL, "*"),
                        new Token(TokenType.IDENT, "y")
                ),
                tokenize("x*y")
        );
    }

    @Test
    void recognizesAssignmentExpression() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "x"),
                        new Token(TokenType.ASSIGN, "="),
                        new Token(TokenType.INT, "5")
                ),
                tokenize("x=5")
        );
    }

    @Test
    void recognizesStatementWithSemicolon() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "value"),
                        new Token(TokenType.SEMICOLON, ";")
                ),
                tokenize("value;")
        );
    }

    @Test
    void handlesNewlineBetweenIdentifiers() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "first"),
                        new Token(TokenType.IDENT, "second")
                ),
                tokenize("first\nsecond")
        );
    }

    @Test
    void handlesTabBetweenIdentifiers() {
        assertEquals(
                List.of(
                        new Token(TokenType.IDENT, "left"),
                        new Token(TokenType.IDENT, "right")
                ),
                tokenize("left\tright")
        );
    }
}