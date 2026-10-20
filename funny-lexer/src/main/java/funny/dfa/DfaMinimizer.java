package funny.dfa;

import funny.nfa.NfaState;
import funny.token.TokenSpec;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DfaMinimizer {

    private List<Set<DfaState>> initialPartitions(Dfa dfa) {
        Set<DfaState> nonAccepting = new HashSet<>();

        Map<TokenSpec, Set<DfaState>> acceptingGroups = new HashMap<>();

        for (DfaState state : dfa.states()) {
            TokenSpec token = state.getAcceptToken();

            if (token == null) {
                nonAccepting.add(state);
            } else {
                acceptingGroups.computeIfAbsent(token, key -> new HashSet<>()).add(state);
            }
        }

        List<Set<DfaState>> partitions = new ArrayList<>();

        if (!nonAccepting.isEmpty()) {
            partitions.add(nonAccepting);
        }

        partitions.addAll(acceptingGroups.values());

        return partitions;
    }

    private Set<DfaState> findPredecessors(Dfa dfa, Set<DfaState> targetGroup, char c) {

        Set<DfaState> result = new HashSet<>();
        for (DfaState state : dfa.states()) {

            DfaState target = state.getTransitions().get(c);

            if (targetGroup.contains(target)) {
                result.add(state);
            }
        }

        return result;
    }


    public Dfa minimize(Dfa dfa) {

        List<Set<DfaState>> partitions = initialPartitions(dfa);

        Deque<Set<DfaState>> worklist = new ArrayDeque<>(partitions);


        while (!worklist.isEmpty()) {
            Set<DfaState> a = worklist.removeFirst();

            for (int code = 0; code < 128; code++) {
                char c = (char) code;

                Set<DfaState> x = findPredecessors(dfa, a, c);


                if (x.isEmpty()) {
                    continue;
                }

                List<Set<DfaState>> currentPartitions = new ArrayList<>(partitions);

                for (Set<DfaState> y : currentPartitions) {
                    Set<DfaState> intersection = new HashSet<>(y);
                    intersection.retainAll(x);

                    Set<DfaState> difference = new HashSet<>(y);

                    difference.removeAll(x);

                    if (!intersection.isEmpty() && !difference.isEmpty()) {

                        partitions.remove(y);
                        partitions.add(intersection);
                        partitions.add(difference);

                        if (worklist.remove(y)) {

                            worklist.add(intersection);
                            worklist.add(difference);

                        } else {

                            if (intersection.size() <= difference.size()) {
                                worklist.add(intersection);
                            } else {
                                worklist.add(difference);
                            }
                        }
                    }
                }
            }
        }

        List<DfaState> newStates = new ArrayList<>();

        Map<DfaState, DfaState> oldToNew = new HashMap<>();

        for (Set<DfaState> partition : partitions) {
            Set<NfaState> nfaStates = new HashSet<>();

            for (DfaState oldState : partition) {
                nfaStates.addAll(oldState.getNfaStates());
            }


            DfaState newState = new DfaState(newStates.size(), nfaStates);

            DfaState representative = partition.iterator().next();

            newState.setAcceptToken(representative.getAcceptToken());
            newStates.add(newState);

            for (DfaState oldState : partition) {
                oldToNew.put(oldState, newState);
            }
        }

        for (Set<DfaState> partition : partitions) {

            DfaState representative = partition.iterator().next();

            DfaState newState = oldToNew.get(representative);


            for (int code = 0; code < 128; code++) {
                char c = (char) code;
                DfaState oldTarget = representative.getTransitions().get(c);

                DfaState newTarget = oldToNew.get(oldTarget);

                newState.addTransition(c, newTarget);
            }
        }

        DfaState newStart = oldToNew.get(dfa.start());

        DfaState newTrap = oldToNew.get(dfa.trap());

        return new Dfa(newStart, newStates, newTrap);
    }
}