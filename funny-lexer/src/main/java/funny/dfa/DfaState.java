package funny.dfa;

import funny.nfa.NfaState;
import funny.token.TokenSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DfaState {

    private final int id;

    // Одно состояние ДКА - множ сост НКА
    private final Set<NfaState> nfaStates;

    private final Map<Character, DfaState> transitions = new HashMap<>();

    // Если состояние принимающее. здесь будет токен типа INT
    private TokenSpec acceptToken;

    public DfaState(int id, Set<NfaState> nfaStates) {
        this.id = id;
        this.nfaStates = Set.copyOf(nfaStates);
    }

    public int getId() {
        return id;
    }

    public Set<NfaState> getNfaStates() {
        return nfaStates;
    }

    public Map<Character, DfaState> getTransitions() {
        return transitions;
    }

    public void addTransition(char c, DfaState state) {
        transitions.put(c, state);
    }

    public TokenSpec getAcceptToken() {
        return acceptToken;
    }

    public void setAcceptToken(TokenSpec acceptToken) {
        this.acceptToken = acceptToken;
    }

    public boolean isAccepting() {
        return acceptToken != null;
    }

    @Override
    public String toString() {
        return "D" + id;
    }
}