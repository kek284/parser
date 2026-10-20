package funny;

import funny.dfa.Dfa;
import funny.dfa.DfaBuilder;
import funny.dfa.DfaJsonSerializer;
import funny.dfa.DfaMinimizer;
import funny.lexer.Lexer;
import funny.lexer.Token;
import funny.nfa.Nfa;
import funny.nfa.NfaBuilder;
import funny.nfa.NfaState;
import funny.token.TokenSpecs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        NfaBuilder nfaBuilder = new NfaBuilder();
        Nfa nfa = nfaBuilder.buildAll(TokenSpecs.create());


        DfaBuilder dfaBuilder = new DfaBuilder();
        Dfa dfa = dfaBuilder.build(nfa);

        DfaMinimizer minimizer = new DfaMinimizer();

        Dfa minimized = minimizer.minimize(dfa);

        Path output = args.length == 0 ? Path.of("target/dfa.json") : Path.of(args[0]);
        try {
            DfaJsonSerializer.write(minimized, output);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write DFA JSON", exception);
        }

        System.out.println("DFA states before minimization: " + dfa.states().size());

        System.out.println("DFA states after minimization: " + minimized.states().size());

        System.out.println("DFA JSON: " + output);

        System.out.println();

        Lexer lexer = new Lexer(minimized);


        String code = """
                function test() {
                    if (value != 10) {
                        assert value >= 0;
                    }
                }
                """;


        System.out.println("Input:");
        System.out.println(code);
        System.out.println("Tokens:");

        List<Token> tokens = lexer.tokenize(code);

        for (Token token : tokens) {

            System.out.printf(
                    "%-12s \"%s\"%n",
                    token.type(),
                    escape(token.value())
            );
        }
    }

    
    private static String escape(String value) {

        return value
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}