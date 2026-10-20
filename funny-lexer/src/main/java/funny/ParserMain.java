package funny;

import funny.dfa.DfaBuilder;
import funny.dfa.DfaMinimizer;
import funny.lexer.Lexer;
import funny.lexer.LexerException;
import funny.lexer.SourcePosition;
import funny.nfa.NfaBuilder;
import funny.parser.AstPrinter;
import funny.parser.Parser;
import funny.token.TokenSpecs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON в stdout, диагностика в stderr; коды: 0 — успех, 1 — ошибки, 2 — ввод/аргументы. */
public final class ParserMain {
    private ParserMain() {}
    public static void main(String[] args) {
        System.exit(run(args));
    }
    public static int run(String[] args) {
        if (args.length > 1) {
            System.err.println("Использование: java -cp target/classes funny.ParserMain [file.funny | -]");
            return 2;
        }
        try {
            String source = args.length == 0
                    ? Files.readString(defaultInput(), StandardCharsets.UTF_8)
                    : args[0].equals("-")
                    ? new String(System.in.readAllBytes(), StandardCharsets.UTF_8)
                    : Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
           
            var nfa = new NfaBuilder().buildAll(TokenSpecs.create());
            var dfa = new DfaMinimizer().minimize(new DfaBuilder().build(nfa));
            var tokens = new Lexer(dfa).tokenize(source);
            var result = new Parser(tokens, SourcePosition.endOf(source)).parse();
            
            System.out.println(AstPrinter.toJson(result.program()));
            result.diagnostics().forEach(System.err::println);
            return result.success() ? 0 : 1;
        } catch (LexerException exception) {
            System.err.println(exception.getMessage());
            return 1;
        } catch (IOException | java.nio.file.InvalidPathException exception) {
            System.err.println("Ошибка чтения: " + exception.getMessage());
            return 2;
        }
    }

    private static Path defaultInput() throws IOException {
        Path modulePath = Path.of("examples/full.funny");
        if (Files.exists(modulePath)) return modulePath;

        Path workspacePath = Path.of("funny-lexer", "examples", "full.funny");
        if (Files.exists(workspacePath)) return workspacePath;

        throw new IOException("Не найден файл examples/full.funny");
    }
}
