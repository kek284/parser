package funny.nfa;
import funny.token.TokenSpec;

import java.util.*;

public class NfaState {
    private final int id;
    private final Map<Character, Set<NfaState>> transitions = new HashMap<>();
    private final Set<NfaState> epsTransitions = new HashSet<>();
    private TokenSpec acceptToken;

    public NfaState(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Map<Character, Set<NfaState>> getTransitions() {
        return transitions;
    }

    public Set<NfaState> getEpsTransitions() {
        return epsTransitions;
    }
    public void addTransition(Character c, NfaState target){
        transitions.computeIfAbsent(c, k -> new HashSet<>()).add(target);

        }

    public void addEpsTransition(NfaState target) {
        epsTransitions.add(target);
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
    public String toString(){
        return "q" + id;
    }

}
