package funny.nfa;

import funny.regex.RegexParser;
import funny.regex.RegexParser.Token;
import funny.token.TokenSpec;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NfaBuilder {

    private int nextStateId = 0;

    public NfaState newState() {
        return new NfaState(nextStateId++);
    }

    public Nfa buildCharacter(char c) {
        NfaState start = newState();
        NfaState accept = newState();

        start.addTransition(c, accept);

        return new Nfa(start, accept);
    }

    public Nfa concat(Nfa left, Nfa right) {
        left.accept().addEpsTransition(right.start());

        return new Nfa(left.start(),right.accept());
    }

    public Nfa alt(Nfa left, Nfa right) {
        NfaState start = newState();
        NfaState accept = newState();

        start.addEpsTransition(left.start());
        start.addEpsTransition(right.start());

        left.accept().addEpsTransition(accept);
        right.accept().addEpsTransition(accept);

        return new Nfa(start, accept);
    }

    public Nfa star(Nfa a) {
        NfaState start = newState();
        NfaState accept = newState();

        // Ноль повторений
        start.addEpsTransition(accept);

        // Начинаем повторение
        start.addEpsTransition(a.start());

        // Можем закончить
        a.accept().addEpsTransition(accept);

        // Или повторить ещё раз
        a.accept().addEpsTransition(a.start());

        return new Nfa(start, accept);
    }

    public Nfa plus(Nfa a) {
        NfaState start = newState();
        NfaState accept = newState();

        // Хотя бы одно повторение обязательно
        start.addEpsTransition(a.start());

        // Закончить
        a.accept().addEpsTransition(accept);

        // Повторить ещё раз
        a.accept().addEpsTransition(a.start());

        return new Nfa(start, accept);
    }

    private Nfa buildAtom(String value) {

        if (value.startsWith("[") && value.endsWith("]")) {
            return buildCharacterClass(value);
        }

        if (value.length() != 1) {
            throw new IllegalArgumentException(
                    "Invalid atom: " + value
            );
        }

        return buildCharacter(value.charAt(0));
    }

    private char readClassCharacter(String value, int index) {

        char c = value.charAt(index);

        if (c != '\\') {
            return c;
        }

        if (index + 1 >= value.length()) {
            throw new IllegalArgumentException("Invalid escape in character class");
        }
        char next = value.charAt(index + 1);

        return switch (next) {
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            default -> next;
        };
    }

    private Nfa buildCharacterClass(String value) {

        NfaState start = newState();
        NfaState accept = newState();

        Set<Character> characters = parseCharacterClass(value);

        for (char c : characters) {
            start.addTransition(c, accept);
        }

        return new Nfa(start, accept);
    }

    private Set<Character> parseCharacterClass(String value) {

        Set<Character> result = new HashSet<>();

        boolean negated = false;

        // Пропускаем начальную [
        int i = 1;

        if (value.charAt(i) == '^') {
            negated = true;
            i++;
        }

        // Последний символ — ]
        int end = value.length() - 1;

        while (i < end) {
            char first = readClassCharacter(value, i);
            int consumed = value.charAt(i) == '\\' ? 2 : 1;

            int next = i + consumed;

            // Проверяем диапазон A-Z или 0-9
            if (next < end && value.charAt(next) == '-' && next + 1 < end) {

                int rangeEndIndex = next + 1;
                char last = readClassCharacter(value, rangeEndIndex);

                int lastConsumed = value.charAt(rangeEndIndex) == '\\' ? 2 : 1;

                if (first > last) {
                    throw new IllegalArgumentException("Invalid range in " + value);
                }

                for (char c = first; c <= last; c++) {
                    result.add(c);
                }

                i = rangeEndIndex + lastConsumed;

            } else {
                result.add(first);
                i += consumed;
            }
        }

        // [^abc] означает:
        // любой ASCII-символ, кроме a, b, c
        if (negated) {

            Set<Character> inverted = new HashSet<>();

            for (char c = 0; c < 128; c++) {
                if (!result.contains(c)) {
                    inverted.add(c);
                }
            }

            return inverted;
        }

        return result;
    }

    public Nfa build(List<Token> postfix) {

        // Стек уже готовых кусочков
        Deque<Nfa> stack = new ArrayDeque<>();

        for (Token token : postfix) {

            switch (token.type()) {

                case ATOM -> {
                    stack.push(
                            buildAtom(token.value())
                    );
                }

                case CONCAT -> {
                    Nfa right = stack.pop();
                    Nfa left = stack.pop();

                    stack.push(
                            concat(left, right)
                    );
                }

                case ALT -> {
                    Nfa right = stack.pop();
                    Nfa left = stack.pop();

                    stack.push(alt(left, right));
                }

                case STAR -> {
                    Nfa nfa = stack.pop();

                    stack.push(star(nfa));
                }

                case PLUS -> {
                    Nfa nfa = stack.pop();

                    stack.push(plus(nfa));
                }

                default -> throw new IllegalArgumentException("Unexpected postfix token: " + token);
            }
        }

        if (stack.size() != 1) {
            throw new IllegalArgumentException(
                    "Invalid postfix regex"
            );
        }

        return stack.pop();
    }
    public Nfa build(TokenSpec spec) {

        var postfix = RegexParser.toPostfix(spec.regex());


        Nfa nfa = build(postfix);

        nfa.accept().setAcceptToken(spec);

        return nfa;
    }
    public Nfa buildAll(List<TokenSpec> specs) {

        NfaState globalStart = newState();

        for (TokenSpec spec : specs) {


            Nfa tokenNfa = build(spec);

            globalStart.addEpsTransition(tokenNfa.start());
        }

        return new Nfa(globalStart, null);
    }
}