package funny.dfa;

import java.util.List;

public record Dfa(
        DfaState start,
        List<DfaState> states,
        DfaState trap
) {
}