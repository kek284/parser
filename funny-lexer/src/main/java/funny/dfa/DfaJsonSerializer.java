package funny.dfa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DfaJsonSerializer {

    private DfaJsonSerializer() {
    }

    public static String serialize(Dfa dfa) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"alphabet\": \"ASCII\",\n");
        json.append("  \"alphabetSize\": 128,\n");
        json.append("  \"startState\": ")
                .append(dfa.start().getId())
                .append(",\n");
        json.append("  \"trapState\": ")
                .append(dfa.trap().getId())
                .append(",\n");
        json.append("  \"states\": [\n");

        for (int stateIndex = 0; stateIndex < dfa.states().size(); stateIndex++) {
            DfaState state = dfa.states().get(stateIndex);
            json.append("    {\n");
            json.append("      \"id\": ").append(state.getId()).append(",\n");
            json.append("      \"accepting\": ")
                    .append(state.isAccepting())
                    .append(",\n");

            if (state.isAccepting()) {
                json.append("      \"token\": \"")
                        .append(state.getAcceptToken().type())
                        .append("\",\n");
                json.append("      \"skip\": ")
                        .append(state.getAcceptToken().skip())
                        .append(",\n");
            } else {
                json.append("      \"token\": null,\n");
                json.append("      \"skip\": false,\n");
            }

            json.append("      \"transitions\": [\n");
            for (int symbol = 0; symbol < 128; symbol++) {
                json.append("        {\"symbol\": ")
                        .append(symbol)
                        .append(", \"target\": ")
                        .append(state.getTransitions().get((char) symbol).getId())
                        .append("}");
                if (symbol < 127) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("      ]\n");
            json.append("    }");
            if (stateIndex < dfa.states().size() - 1) {
                json.append(',');
            }
            json.append('\n');
        }

        json.append("  ]\n");
        json.append("}\n");
        return json.toString();
    }

    public static void write(Dfa dfa, Path path) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, serialize(dfa));
    }
}