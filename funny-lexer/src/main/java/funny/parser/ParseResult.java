package funny.parser;

import java.util.List;

public record ParseResult(Ast.Program program, List<Diagnostic> diagnostics) {
    public ParseResult { diagnostics = List.copyOf(diagnostics); }
    public boolean success() { return diagnostics.isEmpty(); }
}
