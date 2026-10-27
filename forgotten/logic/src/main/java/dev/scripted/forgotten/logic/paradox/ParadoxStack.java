package dev.scripted.forgotten.logic.paradox;

/**
 * What is in a Paradox Engine slot, reduced to the parts recipes look at.
 *
 * @param item   item id, or empty for an empty slot
 * @param memory the memory source the item carries, or empty
 * @param count  stack size
 */
public record ParadoxStack(String item, String memory, int count) {
    public static final ParadoxStack EMPTY = new ParadoxStack("", "", 0);

    public ParadoxStack {
        item = item == null ? "" : item;
        memory = memory == null ? "" : memory;
    }

    public static ParadoxStack of(String item, int count) {
        return new ParadoxStack(item, "", count);
    }

    public boolean isEmpty() {
        return item.isEmpty() || count <= 0;
    }
}
