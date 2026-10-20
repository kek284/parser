package funny.dfa;

import funny.nfa.NfaState;
import funny.nfa.Nfa;
import funny.token.TokenSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class DfaBuilder {
    public Set<NfaState> epsilonClosure(Set<NfaState> states) {
        Set<NfaState> closure = new HashSet<>(states);
        Queue<NfaState> queue = new ArrayDeque<>(states);

        while (!queue.isEmpty()) {
            NfaState current = queue.remove();
            for (NfaState next :
                    current.getEpsTransitions()) {

                if (closure.add(next)) {
                    queue.add(next);
                }
            }
        }
        return closure;
    }

    //Если НКА сейчас может находиться в q0,q1,q4, куда он может попасть прочитав символ c
    public Set<NfaState> move(Set<NfaState> states, char c) {
        Set<NfaState> result = new HashSet<>();

        for (NfaState state : states) {
            Set<NfaState> targets = state.getTransitions().get(c);

            if (targets != null) {
                result.addAll(targets);
            }
        }

        return result;
    }

    //function - слово или функция
    private TokenSpec findAcceptToken(Set<NfaState> states) {
        TokenSpec best = null;
        for (NfaState state : states) {
            TokenSpec token = state.getAcceptToken();
            if (token == null) {
                continue;
            }

            if (best == null || token.priority() < best.priority()) {

                best = token;
            }
        }
        return best;
    }

    public Dfa build(Nfa nfa) {
        //D0, D1, D2, ...
        List<DfaState> dfaStates = new ArrayList<>();

        //{q0,q1,q8} -> D5
        Map<Set<NfaState>, DfaState> stateMap = new HashMap<>();

        //очередь состояний D1,D4, D17
        Queue<DfaState> queue = new ArrayDeque<>();

        Set<NfaState> startSet = new HashSet<>();
        startSet.add(nfa.start());

        Set<NfaState> startClosure = epsilonClosure(startSet);

        DfaState startDfa = new DfaState(0, startClosure);
        startDfa.setAcceptToken(findAcceptToken(startClosure));

        dfaStates.add(startDfa);
        stateMap.put(startDfa.getNfaStates(), startDfa);
        queue.add(startDfa);

        while (!queue.isEmpty()) {

            DfaState current = queue.remove();
            for (int code = 0; code < 128; code++) {

                char c = (char) code;

                Set<NfaState> moved = move(current.getNfaStates(), c);
                if (moved.isEmpty()) {
                    continue;
                }

                Set<NfaState> closure = epsilonClosure(moved);

                //проверяем существует ли уже состояние ДКА оответствующее точно этому множеству НКА
                DfaState next = stateMap.get(closure);
                if (next == null) {

                    next = new DfaState(dfaStates.size(), closure);
                    next.setAcceptToken(findAcceptToken(closure));
                    dfaStates.add(next);

                    stateMap.put(next.getNfaStates(), next);
                    queue.add(next);
                }
                current.addTransition(c, next);
            }

        }
        DfaState trap = new DfaState(dfaStates.size(), Set.of());
        for (int code = 0; code < 128; code++) {

            char c = (char) code;
            trap.addTransition(c, trap);

        }
        for (DfaState state : dfaStates) {

            for (int code = 0; code < 128; code++) {
                char c = (char) code;

                if (!state.getTransitions().containsKey(c)) {
                    state.addTransition(c, trap);
                }
            }
        }
        dfaStates.add(trap);

        return new Dfa(startDfa, dfaStates, trap);
    }
}
