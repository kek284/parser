package funny.lexer;

import funny.dfa.Dfa;
import funny.dfa.DfaState;
import funny.token.TokenSpec;

import java.util.ArrayList;
import java.util.List;

public class Lexer {

    private final Dfa dfa;

    public Lexer(Dfa dfa) {
        this.dfa = dfa;
    }

    public List<Token> tokenize(String input) {

        List<Token> result = new ArrayList<>();

        // Текущая позиция начала нового токена.
        int position = 0;
        int line = 1, column = 1;

        while (position < input.length()) {

            // Каждый новый токен начинаем
            // со стартового состояния ДКА.
            DfaState state = dfa.start();

            // Последний найденный принимающий токен.
            TokenSpec lastAcceptToken = null;

            // Позиция сразу после последнего
            // принятого символа.
            int lastAcceptPosition = -1;

            int current = position;

            /*
             * Идём вперёд, пока ДКА способен
             * продолжать текущий токен.
             */
            while (current < input.length()) {

                char c = input.charAt(current);

                /*
                 * По условию наш алфавит ASCII.
                 *
                 * Всё >= 128 недопустимо.
                 */
                if (c >= 128) {
                    break;
                }

                DfaState next =
                        state.getTransitions().get(c);

                /*
                 * В полном DFA next вообще не должен
                 * быть null, но проверка не мешает.
                 */
                if (next == null || next == dfa.trap()) {
                    break;
                }

                state = next;
                current++;

                /*
                 * Если новое состояние принимающее,
                 * запоминаем его.
                 *
                 * Но НЕ останавливаемся:
                 * ищем более длинный токен.
                 */
                if (state.isAccepting()) {
                    lastAcceptToken =
                            state.getAcceptToken();

                    lastAcceptPosition =
                            current;
                }
            }

            /*
             * Если мы вообще ни разу не были
             * в принимающем состоянии,
             * токен с этой позиции построить нельзя.
             */
            if (lastAcceptToken == null) {

                char bad = input.charAt(position);

                String shown;

                if (bad == '\n') {
                    shown = "\\n";
                } else if (bad == '\r') {
                    shown = "\\r";
                } else if (bad == '\t') {
                    shown = "\\t";
                } else {
                    shown = String.valueOf(bad);
                }

                throw new LexerException(
                        "Unexpected character '"
                                + shown
                                + "' at position "
                                + position + " (" + line + ":" + column + ")"
                );
            }

            /*
             * Берём максимально длинный
             * успешно распознанный фрагмент.
             */
            String value =
                    input.substring(
                            position,
                            lastAcceptPosition
                    );

                if (lastAcceptToken.type() == funny.token.TokenType.INT
                    && value.equals("0")
                    && lastAcceptPosition < input.length()
                    && Character.isDigit(input.charAt(lastAcceptPosition))) {
                throw new LexerException(
                    "Invalid integer with leading zero at position "
                        + position + " (" + line + ":" + column + ")"
                );
                }


            /*
             * WS и COMMENT имеют skip=true,
             * поэтому наружу их не возвращаем.
             */
            if (!lastAcceptToken.skip()) {

                result.add(
                        new Token(lastAcceptToken.type(), value,
                                new SourcePosition(position, line, column))
                );
            }


            /*
             * Следующий токен начинается там,
             * где закончился предыдущий.
             */
            for (int i = position; i < lastAcceptPosition; i++) {
                char c = input.charAt(i);
                if (c == '\r') { line++; column = 1; }
                else if (c == '\n') {
                    if (i == 0 || input.charAt(i - 1) != '\r') line++;
                    column = 1;
                } else column++;
            }
            position = lastAcceptPosition;
        }

        return result;
    }
}