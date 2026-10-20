package funny;

import funny.dfa.Dfa;
import funny.dfa.DfaBuilder;
import funny.dfa.DfaJsonSerializer;
import funny.dfa.DfaMinimizer;
import funny.nfa.Nfa;
import funny.nfa.NfaBuilder;
import funny.regex.RegexParser;
import funny.token.TokenSpecs;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexParserTest {

    @Test
    void convertsGroupingAlternationAndRepetitionToPostfix() {
        assertEquals(
                "a b c | * .",
                RegexParser.format(RegexParser.toPostfix("a(b|c)*"))
        );
    }

    @Test
    void decodesEscapedCharacters() {
        assertEquals(
                "\\n \\t . + .",
                RegexParser.format(RegexParser.toPostfix("\\n\\t\\+"))
        );
    }

    @Test
    void rejectsMalformedRegex() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RegexParser.toPostfix("\\")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RegexParser.toPostfix("(abc")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RegexParser.toPostfix("[abc")
        );
    }

    @Test
    void writesSerializedDfaToFile() throws Exception {
        Nfa nfa = new NfaBuilder().buildAll(TokenSpecs.create());
        Dfa dfa = new DfaMinimizer().minimize(new DfaBuilder().build(nfa));
        Path output = Files.createTempFile("funny-dfa-", ".json");

        try {
            DfaJsonSerializer.write(dfa, output);
            String json = Files.readString(output);

            assertTrue(Files.size(output) > 0);
            assertTrue(json.startsWith("{\n"));
            assertTrue(json.endsWith("}\n"));
        } finally {
            Files.deleteIfExists(output);
        }
    }
}