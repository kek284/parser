package funny.lexer;

public record SourcePosition(int offset, int line, int column) {
    public static SourcePosition endOf(String source) {
        int line = 1, column = 1;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '\r') { line++; column = 1; }
            else if (c == '\n') {
                if (i == 0 || source.charAt(i - 1) != '\r') line++;
                column = 1;
            } else column++;
        }
        return new SourcePosition(source.length(), line, column);
    }
    @Override public String toString() { return line + ":" + column; }
}
