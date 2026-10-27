package dev.scripted.forgotten.logic.paradox;

import java.util.Objects;

/**
 * One slot of a Paradox recipe. Matches an item id and, optionally, the memory the item holds:
 * a Memory extracted from a creeper is the same item as one extracted from water, but they are
 * different ingredients.
 *
 * @param item    item id, e.g. "forgotten:memory"
 * @param memory  required memory source id, or empty for any
 * @param count   minimum stack size consumed
 */
public record ParadoxIngredient(String item, String memory, int count) {
    public ParadoxIngredient {
        Objects.requireNonNull(item, "item");
        memory = memory == null ? "" : memory;
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1");
        }
    }

    public static ParadoxIngredient of(String item) {
        return new ParadoxIngredient(item, "", 1);
    }

    public static ParadoxIngredient memory(String memoryItem, String source) {
        return new ParadoxIngredient(memoryItem, source, 1);
    }

    public boolean matches(ParadoxStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!item.equals(stack.item()) || stack.count() < count) {
            return false;
        }
        return memory.isEmpty() || memory.equals(stack.memory());
    }

    /** Recipes asking for a specific memory are more specific than ones that accept any. */
    public int specificity() {
        return memory.isEmpty() ? 1 : 2;
    }
}
