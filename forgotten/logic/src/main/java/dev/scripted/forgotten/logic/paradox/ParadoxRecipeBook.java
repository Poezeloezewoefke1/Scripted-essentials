package dev.scripted.forgotten.logic.paradox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Finds the recipe for a set of Paradox Engine inputs. When several match, the most specific
 * wins: a recipe for "Memory of Creeper" beats one for "any Memory".
 */
public final class ParadoxRecipeBook {
    private final List<ParadoxRecipe> recipes = new ArrayList<>();

    public ParadoxRecipeBook() {
    }

    public ParadoxRecipeBook(Collection<ParadoxRecipe> recipes) {
        this.recipes.addAll(recipes);
    }

    public void replaceAll(Collection<ParadoxRecipe> next) {
        recipes.clear();
        recipes.addAll(next);
    }

    public List<ParadoxRecipe> all() {
        return List.copyOf(recipes);
    }

    public int size() {
        return recipes.size();
    }

    public Lookup find(ParadoxStack subject, ParadoxStack contradiction, ParadoxStack catalyst, int rankLevel, int depth) {
        List<Candidate> fits = new ArrayList<>();
        for (ParadoxRecipe recipe : recipes) {
            ParadoxRecipe.Fit fit = recipe.fit(subject, contradiction, catalyst);
            if (fit != ParadoxRecipe.Fit.NONE) {
                fits.add(new Candidate(recipe, fit));
            }
        }
        if (fits.isEmpty()) {
            return new Lookup(Outcome.NO_RECIPE, null, ParadoxRecipe.Fit.NONE);
        }
        fits.sort(Comparator.comparingInt((Candidate c) -> c.recipe.specificity()).reversed()
                .thenComparing(c -> c.recipe.id()));
        Candidate best = fits.get(0);
        if (best.recipe.rank() > rankLevel) {
            return new Lookup(Outcome.RANK_TOO_LOW, best.recipe, best.fit);
        }
        if (best.recipe.minDepth() > depth) {
            return new Lookup(Outcome.TOO_SHALLOW, best.recipe, best.fit);
        }
        return new Lookup(Outcome.OK, best.recipe, best.fit);
    }

    private record Candidate(ParadoxRecipe recipe, ParadoxRecipe.Fit fit) {
    }

    public enum Outcome {
        OK, NO_RECIPE, RANK_TOO_LOW, TOO_SHALLOW
    }

    public record Lookup(Outcome outcome, ParadoxRecipe recipe, ParadoxRecipe.Fit fit) {
        public boolean ok() {
            return outcome == Outcome.OK;
        }
    }
}
