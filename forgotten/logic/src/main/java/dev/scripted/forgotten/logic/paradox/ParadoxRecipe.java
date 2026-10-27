package dev.scripted.forgotten.logic.paradox;

import java.util.Objects;

/**
 * Subject, Contradiction and an optional Catalyst become a Result. Loaded from data packs
 * ({@code data/<namespace>/paradox/*.json}), so packs can teach the Engine new contradictions.
 *
 * @param id            recipe id
 * @param subject       the thing whose rules change
 * @param contradiction the memory or concept that contradicts them
 * @param catalyst      third concept for three-way combinations, or null
 * @param result        result item id
 * @param resultCount   result stack size
 * @param resultData    extra data copied onto the result (e.g. the memory a Memory of X holds)
 * @param debt          causal debt charged to the player
 * @param rank          minimum Architect rank level, 1-5
 * @param minDepth      minimum nesting depth the Engine must be at (0 = anywhere)
 * @param ordered       whether subject and contradiction may be swapped
 */
public record ParadoxRecipe(String id, ParadoxIngredient subject, ParadoxIngredient contradiction,
                            ParadoxIngredient catalyst, String result, int resultCount, String resultData,
                            double debt, int rank, int minDepth, boolean ordered) {
    public ParadoxRecipe {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(contradiction, "contradiction");
        Objects.requireNonNull(result, "result");
        resultData = resultData == null ? "" : resultData;
        if (resultCount < 1) {
            throw new IllegalArgumentException("resultCount must be at least 1");
        }
        if (rank < 1 || rank > 5) {
            throw new IllegalArgumentException("rank must be 1-5");
        }
    }

    /** Whether the ingredients fit this recipe, and in which orientation. */
    public Fit fit(ParadoxStack s, ParadoxStack c, ParadoxStack k) {
        boolean catalystOk = catalyst == null ? k == null || k.isEmpty() : catalyst.matches(k);
        if (!catalystOk) {
            return Fit.NONE;
        }
        if (subject.matches(s) && contradiction.matches(c)) {
            return Fit.STRAIGHT;
        }
        if (!ordered && subject.matches(c) && contradiction.matches(s)) {
            return Fit.SWAPPED;
        }
        return Fit.NONE;
    }

    public int specificity() {
        return subject.specificity() + contradiction.specificity() + (catalyst == null ? 0 : 3);
    }

    public enum Fit {
        NONE, STRAIGHT, SWAPPED
    }
}
