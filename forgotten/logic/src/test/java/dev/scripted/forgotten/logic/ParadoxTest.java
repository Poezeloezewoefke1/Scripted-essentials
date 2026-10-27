package dev.scripted.forgotten.logic;

import dev.scripted.forgotten.logic.paradox.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParadoxTest {
    private static final ParadoxRecipe ANY_MEMORY = new ParadoxRecipe("any", ParadoxIngredient.of("forgotten:memory"),
            ParadoxIngredient.of("forgotten:concept_absence"), null, "forgotten:residue", 1, "", 1, 1, 0, true);
    private static final ParadoxRecipe BLOOM = new ParadoxRecipe("bloom",
            ParadoxIngredient.memory("forgotten:memory", "minecraft:creeper"),
            ParadoxIngredient.memory("forgotten:memory", "minecraft:oak_sapling"), null,
            "forgotten:bloom_creeper_spawn_egg", 1, "", 12, 2, 0, true);
    private static final ParadoxRecipe SINGULARITY = new ParadoxRecipe("singularity",
            ParadoxIngredient.of("forgotten:concept_gravity"), ParadoxIngredient.of("forgotten:concept_recursion"), null,
            "forgotten:singularity", 1, "", 20, 3, 0, false);
    private static final ParadoxRecipe DEEP = new ParadoxRecipe("deep",
            ParadoxIngredient.of("minecraft:diamond"), ParadoxIngredient.of("forgotten:concept_absence"), null,
            "forgotten:memory", 1, "minecraft:diamond", 0, 1, 1, true);

    private final ParadoxRecipeBook book = new ParadoxRecipeBook(List.of(ANY_MEMORY, BLOOM, SINGULARITY, DEEP));

    private static ParadoxStack mem(String source) {
        return new ParadoxStack("forgotten:memory", source, 1);
    }

    @Test
    void specificRecipeBeatsGeneral() {
        var r = book.find(mem("minecraft:creeper"), mem("minecraft:oak_sapling"), ParadoxStack.EMPTY, 2, 0);
        assertTrue(r.ok());
        assertEquals("bloom", r.recipe().id());
    }

    @Test
    void unorderedRecipesAcceptEitherOrder() {
        var g = ParadoxStack.of("forgotten:concept_gravity", 1);
        var rec = ParadoxStack.of("forgotten:concept_recursion", 1);
        assertEquals(ParadoxRecipe.Fit.SWAPPED, book.find(rec, g, ParadoxStack.EMPTY, 5, 0).fit());
        assertEquals("singularity", book.find(rec, g, ParadoxStack.EMPTY, 5, 0).recipe().id());
    }

    @Test
    void orderedRecipesDoNot() {
        var r = book.find(mem("minecraft:oak_sapling"), mem("minecraft:creeper"), ParadoxStack.EMPTY, 5, 0);
        assertNotEquals("bloom", r.recipe() == null ? "" : r.recipe().id());
    }

    @Test
    void rankAndDepthGate() {
        var g = ParadoxStack.of("forgotten:concept_gravity", 1);
        var rec = ParadoxStack.of("forgotten:concept_recursion", 1);
        assertEquals(ParadoxRecipeBook.Outcome.RANK_TOO_LOW, book.find(g, rec, ParadoxStack.EMPTY, 2, 0).outcome());
        var d = ParadoxStack.of("minecraft:diamond", 64);
        var a = ParadoxStack.of("forgotten:concept_absence", 1);
        assertEquals(ParadoxRecipeBook.Outcome.TOO_SHALLOW, book.find(d, a, ParadoxStack.EMPTY, 5, 0).outcome());
        assertTrue(book.find(d, a, ParadoxStack.EMPTY, 5, 1).ok());
    }

    @Test
    void catalystMustMatchOrBeAbsent() {
        var g = ParadoxStack.of("forgotten:concept_gravity", 1);
        var rec = ParadoxStack.of("forgotten:concept_recursion", 1);
        assertEquals(ParadoxRecipeBook.Outcome.NO_RECIPE,
                book.find(g, rec, ParadoxStack.of("minecraft:dirt", 1), 5, 0).outcome());
    }

    @Test
    void debtTiersAndRanks() {
        assertEquals(DebtTier.NONE, DebtTier.of(0));
        assertEquals(DebtTier.WHISPERS, DebtTier.of(20));
        assertEquals(DebtTier.BLEED, DebtTier.of(9999));
        assertEquals(0.0, CausalDebt.correctionChance(DebtTier.REARRANGEMENT));
        assertTrue(CausalDebt.correctionChance(DebtTier.BLEED) > CausalDebt.correctionChance(DebtTier.CORRECTIONS));
        assertEquals(75.0, CausalDebt.afterDeath(100), 1e-9);
        assertEquals(100.0, CausalDebt.afterTime(100, 0, 100, 100), 1e-9, "recent edits keep debt");
        assertTrue(CausalDebt.afterTime(100, 0, 100_000, 60_000) < 100);
        assertEquals(ArchitectRank.OBSERVER, ArchitectRank.of(0));
        assertEquals(ArchitectRank.PARADOX_LORD, ArchitectRank.of(10_000));
        assertEquals(25, ArchitectRank.OBSERVER.toNext(0));
        assertEquals(ArchitectRank.REWRITER, ArchitectRank.ofLevel(3));
    }
}
