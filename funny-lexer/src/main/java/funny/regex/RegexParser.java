package funny.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class RegexParser {

    private RegexParser() {}
    public enum Type {
        ATOM,
        ALT,
        CONCAT,
        STAR,
        PLUS,
        LPAREN,
        RPAREN
    }
    public record Token(Type type, String value) {
        @Override
        public String toString() {
            return value;
        }
    }
    public static List<Token> toPostfix(String regex) {
        List<Token> tokens = tokenize(regex);
        List<Token> tok_with_Concat = insertConcatenation(tokens);

        return algos(tok_with_Concat);
    }
    public static List<Token> tokenize(String regex) {
        List<Token> result = new ArrayList<>();

        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);

            switch(c) {
                case '(' ->  result.add(new Token(Type.LPAREN, "("));
                case ')' -> result.add(new Token(Type.RPAREN, ")"));
                case '|' -> result.add(new Token(Type.ALT, "|"));
                case '*' -> result.add(new Token(Type.STAR, "*"));
                case '+' -> result.add(new Token(Type.PLUS, "+"));

                case '\\' -> {
                    if (i + 1 >= regex.length()) {
                        throw new IllegalArgumentException(
                                "Backslash at end of regex"
                        );
                    }
                    char nxt = regex.charAt(++i);
                    String value = switch(nxt) {
                        case 'n' -> "\n";
                        case 'r' -> "\r";
                        case 't' -> "\t";
                        default -> String.valueOf(nxt);
                    };
                    result.add(new Token(Type.ATOM, value));

                }
                case '[' -> {
                    int start = i;

                    i++;

                    boolean escaped = false;

                    while (i < regex.length()) {
                        char current = regex.charAt(i);

                        if (escaped) {
                            escaped = false;
                        } else if (current == '\\') {
                            escaped = true;
                        } else if (current == ']') {
                            break;
                        }

                        i++;
                    }

                    if (i >= regex.length()) {
                        throw new IllegalArgumentException("Unclosed " + regex);
                    }

                    String charClass = regex.substring(start, i + 1);

                    result.add(new Token(Type.ATOM, charClass)

                    );

                }
                default -> result.add(
                        new Token(Type.ATOM, String.valueOf(c)));
            }
        } return result;
    }  private static List<Token> insertConcatenation(List<Token> tokens) {
        List<Token> result = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token current = tokens.get(i);

            if (!result.isEmpty()) {
                Token previous = result.get(result.size() - 1);

                if (needsConcat(previous, current)) {
                    result.add(new Token(Type.CONCAT, "."));
                }
            }

            result.add(current);
        }

        return result;
    }
    private static boolean needsConcat(Token left, Token right) {
        boolean leftCanEndExpression =
                left.type() == Type.ATOM
                        || left.type() == Type.RPAREN
                        || left.type() == Type.STAR
                        || left.type() == Type.PLUS;

        boolean rightCanStartExpression =
                right.type() == Type.ATOM
                        || right.type() == Type.LPAREN;

        return leftCanEndExpression && rightCanStartExpression;//атом и атом a . b или же a . ( true && true
    }

    private static List<Token> algos(List<Token> tokens) {
        List<Token> output = new ArrayList<>();
        Stack<Token> operators = new Stack<>();

        for (Token token : tokens) {

            switch (token.type()) {

                case ATOM -> output.add(token);

                /*
                 * * и + относятся к предыдущему выражению.
                 */
                case STAR, PLUS -> output.add(token);

                case CONCAT, ALT -> {
                    while (!operators.isEmpty()
                            && operators.peek().type() != Type.LPAREN
                            && precedence(operators.peek())
                            >= precedence(token)) {

                        output.add(operators.pop());
                    }

                    operators.push(token); /*после того как вытолкнули все операторы кладем текущий*/
                }

                case LPAREN -> operators.push(token);

                case RPAREN -> {
                    while (!operators.isEmpty()
                            && operators.peek().type() != Type.LPAREN) {

                        output.add(operators.pop());
                    }

                    if (operators.isEmpty()) {
                        throw new IllegalArgumentException("Unmatched ')'");
                    }

                    operators.pop();
                }
            }
        }

        while (!operators.isEmpty()) {
            Token token = operators.pop();

            if (token.type() == Type.LPAREN) {
                throw new IllegalArgumentException(
                        "Unmatched '('");
            }

            output.add(token);
        }

        return output;
    }

    private static int precedence(Token token) {
        return switch (token.type()) {
            case ALT -> 1;
            case CONCAT -> 2;
            case STAR, PLUS -> 3;
            default -> 0;
        };
    }

    public static String format(List<Token> tokens) {
        StringBuilder result = new StringBuilder();

        for (Token token : tokens) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            String value = token.value();

            switch (value) {
                case "\n" -> result.append("\\n");
                case "\r" -> result.append("\\r");
                case "\t" -> result.append("\\t");
                default -> result.append(value);
            }
        }

        return result.toString();
    }
}
