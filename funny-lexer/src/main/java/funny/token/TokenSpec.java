package funny.token;

public record TokenSpec(
        TokenType type,
        String regex,
        int priority,
        boolean skip
) {
}
