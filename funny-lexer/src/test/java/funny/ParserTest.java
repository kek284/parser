package funny;

import funny.dfa.DfaBuilder;
import funny.dfa.DfaMinimizer;
import funny.lexer.*;
import funny.nfa.NfaBuilder;
import funny.parser.*;
import funny.token.TokenSpecs;
import funny.token.TokenType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import static funny.parser.Ast.*;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {
    private static Lexer lexer;
    @BeforeAll static void setup() {
        lexer = new Lexer(new DfaMinimizer().minimize(
                new DfaBuilder().build(new NfaBuilder().buildAll(TokenSpecs.create()))));
    }
    private ParseResult parse(String source) {
        return new Parser(lexer.tokenize(source), SourcePosition.endOf(source)).parse();
    }
    private Expression value(String expression) {
        var result = parse("function f() returns r:int { r = " + expression + "; }");
        assertTrue(result.success(), result.diagnostics().toString());
        return ((Assign) result.program().functions().getFirst().body().statements().getFirst()).value();
    }
    @Test void minimalAstMatchesSnapshot() throws Exception {
        String source = Files.readString(Path.of("src/test/resources/parser/minimal.funny"));
        var result = parse(source);
        assertTrue(result.success());
        assertEquals(Files.readString(Path.of("src/test/resources/parser/minimal.json")).strip(),
                AstPrinter.toJson(result.program()));
    }
    @Test void fullProgram() throws Exception {
        var result = parse(Files.readString(Path.of("examples/full.funny")));
        assertTrue(result.success(), result.diagnostics().toString());
        assertEquals(2, result.program().functions().size());
        Function demo = result.program().functions().get(1);
        assertEquals("int[]", demo.parameters().getFirst().type());
        assertEquals(2, demo.returns().size());
        assertNotNull(demo.requires()); assertNotNull(demo.ensures());
        assertNull(demo.locals().get(1).type());
        assertInstanceOf(While.class, demo.body().statements().get(4));
        assertNotNull(((While) demo.body().statements().get(4)).invariant());
        assertNotNull(((If) demo.body().statements().get(5)).elseBranch());
    }
    @Test void precedenceAndAssociativity() {
        var tree = (Binary) value("10 - 3 - 2 * 4 / 2 + -1");
        assertEquals("+", tree.operator());
        assertEquals("-", ((Binary) tree.left()).operator());
        assertEquals("-", ((Binary) ((Binary) tree.left()).left()).operator());
        assertEquals("/", ((Binary) ((Binary) tree.left()).right()).operator());
        assertInstanceOf(Unary.class, tree.right());
        var grouped = (Binary) value("(1 + 2) * 3");
        assertEquals("*", grouped.operator());
        assertEquals("+", ((Binary) grouped.left()).operator());
    }
    @Test void logicAndImplication() {
        var tree = (Binary) value("not x < 2 and true or false -> true -> false");
        assertEquals("->", tree.operator());
        assertEquals("->", ((Binary) tree.right()).operator());
        var or = (Binary) tree.left(); assertEquals("or", or.operator());
        var and = (Binary) or.left(); assertEquals("and", and.operator());
        assertEquals("<", ((Binary) ((Unary) and.left()).operand()).operator());
    }
    @Test void allComparisons() {
        for (String op : List.of("==", "!=", "<", "<=", ">", ">="))
            assertEquals(op, ((Binary) value("x " + op + " y")).operator());
    }
    @Test void callsArraysAndHugeNumbers() {
        var call = (Call) value("f(length(a), g(), a[i][j], 9999999999999999999999999)");
        assertEquals(4, call.arguments().size());
        assertEquals("length", ((Call) call.arguments().getFirst()).name());
        assertInstanceOf(ArrayRead.class, ((ArrayRead) call.arguments().get(2)).array());
        assertEquals("9999999999999999999999999", ((IntegerLiteral) call.arguments().get(3)).value());
    }
    @Test void desugarsNestedUpdate() {
        var result = parse("function f() returns a:int[] { a[i][j] = 7; }");
        assertTrue(result.success());
        var assign = (Assign) result.program().functions().getFirst().body().statements().getFirst();
        assertEquals(List.of("a"), assign.targets());
        var outer = (ArraySet) assign.value();
        assertEquals("a", ((Name) outer.array()).name());
        assertEquals("i", ((Name) outer.index()).name());
        var inner = (ArraySet) outer.value();
        assertEquals("j", ((Name) inner.index()).name());
        assertEquals(new ArrayRead(outer.array(), outer.index(), outer.position()), inner.array());
        assertEquals("7", ((IntegerLiteral) inner.value()).value());
    }
    @Test void tupleAssignment() {
        var result = parse("function f() returns x:int, y:int { x,y = pair(); }");
        assertTrue(result.success());
        assertEquals(List.of("x", "y"), ((Assign) result.program().functions().getFirst()
                .body().statements().getFirst()).targets());
        assertFalse(parse("function f() returns x:int { x,y = 1; }").success());
    }
    @Test void optionalClausesAndEmptyBlocks() {
        var result = parse("function f() returns r:int { if (true) {} while (false) {} {} }");
        assertTrue(result.success());
        Function f = result.program().functions().getFirst();
        assertNull(f.requires()); assertNull(f.ensures());
        assertNull(((If) f.body().statements().getFirst()).elseBranch());
        assertNull(((While) f.body().statements().get(1)).invariant());
    }
    @Test void tokenPositionsIncludeSkippedTextAndCrLf() {
        var tokens = lexer.tokenize("// hi\r\n  x\r y\n\tz");
        assertEquals(new SourcePosition(9, 2, 3), tokens.get(0).position());
        assertEquals(new SourcePosition(12, 3, 2), tokens.get(1).position());
        assertEquals(new SourcePosition(15, 4, 2), tokens.get(2).position());
        assertEquals(new SourcePosition(16, 4, 3), SourcePosition.endOf("// hi\r\n  x\r y\n\tz"));
    }
    @Test void extraKeywordsHavePriorityOverIdentifiers() {
        var tokens = lexer.tokenize("requires ensures uses int true false not and or -> integer requiresX");
        assertEquals(List.of(TokenType.REQUIRES, TokenType.ENSURES, TokenType.USES,
                TokenType.TYPE_INT, TokenType.TRUE, TokenType.FALSE, TokenType.NOT,
                TokenType.AND, TokenType.OR, TokenType.IMPLIES, TokenType.IDENT, TokenType.IDENT),
                tokens.stream().map(Token::type).toList());
    }
    @Test void negativeFixturesHaveExpectedDiagnostics() throws Exception {
        for (String name : List.of("empty", "unclosed", "extra", "operator", "missing_semicolon")) {
            String source = Files.readString(Path.of("src/test/resources/parser/" + name + ".funny"));
            var result = assertTimeoutPreemptively(Duration.ofSeconds(2), () -> parse(source));
            assertFalse(result.success(), name);
            String expected = Files.readString(Path.of("src/test/resources/parser/" + name + ".error")).strip();
            assertEquals(expected, result.diagnostics().getFirst().toString(), name);
        }
    }
    @Test void panicModePreservesFollowingStatementAndFunction() throws Exception {
        var result = parse(Files.readString(Path.of("examples/errors.funny")));
        assertEquals(3, result.diagnostics().size());
        assertEquals(2, result.program().functions().size());
        var statements = result.program().functions().getFirst().body().statements();
        assertEquals(1, statements.size());
        assertEquals("5", ((IntegerLiteral) ((Assign) statements.getFirst()).value()).value());
    }
    @Test void recoversAtNextFunctionEvenWithoutClosingBrace() {
        var result = parse("function bad() returns r:int { r = 1; function good() returns r:int {}");
        assertFalse(result.success());
        assertEquals("good", result.program().functions().getFirst().name());
    }
    @Test void malformedInputsTerminate() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            var random = new Random(42);
            String[] fragments = {"x", "=", ";", "(", ")", "[", "]", "{", "}", "if", "else", "assert", "1", "+"};
            for (int n = 0; n < 300; n++) {
                var body = new StringBuilder("function f() returns r:int { ");
                for (int i = 0; i < 30; i++) body.append(fragments[random.nextInt(fragments.length)]).append(' ');
                parse(body.append('}').toString());
            }
        });
    }
    @Test void rejectsUnsupportedAndMalformedSyntax() {
        for (String source : List.of("function f(x) returns r:int {}", "function f() {}",
                "function f() returns r:int { if(true) r=1; }",
                "function f() returns r:int { r=1<2<3; }",
                "function f() returns r:int { r=length; }",
                "function f() returns r:int { r=f(1,); }",
                "function f() returns r:int { r=a[1; }")) assertFalse(parse(source).success(), source);
    }
    @Test void lexicalErrorsHavePositions() {
        assertTrue(assertThrows(LexerException.class, () -> lexer.tokenize("\n @")).getMessage().contains("2:2"));
        assertTrue(assertThrows(LexerException.class, () -> lexer.tokenize("\n 01")).getMessage().contains("2:2"));
    }
    @Test void astListsAreImmutable() {
        var result = parse("function f() returns r:int {}");
        assertThrows(UnsupportedOperationException.class, () -> result.program().functions().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.diagnostics().clear());
    }
}
