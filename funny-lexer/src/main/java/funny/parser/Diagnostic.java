package funny.parser;

import funny.lexer.SourcePosition;

public record Diagnostic(SourcePosition position, String message) {
    @Override public String toString() { return position + ": " + message; }
}
