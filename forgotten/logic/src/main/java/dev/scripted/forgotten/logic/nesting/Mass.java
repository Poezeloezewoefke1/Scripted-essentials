package dev.scripted.forgotten.logic.nesting;

/**
 * Mass Debt. Carrying things up out of a deeper layer costs: every item has a mass, and moving
 * mass upward across depths is limited. Things refined into Memories at depth weigh nothing.
 */
public final class Mass {
    /** Mass a player can carry up one depth without consequence. */
    public static final double CAPACITY = 400.0;

    private Mass() {
    }

    /** Mass of one item, by id. Heavy things are heavy; ideas and memories weigh nothing. */
    public static double of(String item) {
        String p = item.contains(":") ? item.substring(item.indexOf(':') + 1) : item;
        if (item.startsWith("forgotten:memory") || item.startsWith("forgotten:concept_")
                || p.equals("feather") || p.equals("paper") || p.equals("string")) {
            return 0.0;
        }
        if (p.equals("bedrock") || p.equals("netherite_block") || p.equals("anvil")) {
            return 40.0;
        }
        if (p.endsWith("_block") && (p.startsWith("iron") || p.startsWith("gold") || p.startsWith("diamond")
                || p.startsWith("emerald") || p.startsWith("copper") || p.startsWith("netherite") || p.startsWith("raw_"))) {
            return 18.0;
        }
        if (p.contains("netherite")) {
            return 8.0;
        }
        if (p.endsWith("_ore") || p.startsWith("raw_") || p.endsWith("_ingot") || p.equals("diamond")
                || p.equals("emerald") || p.equals("ancient_debris")) {
            return 2.0;
        }
        if (p.contains("obsidian") || p.contains("deepslate") || p.contains("stone") || p.contains("cobble")) {
            return 1.5;
        }
        return 0.5;
    }

    public enum Outcome {
        /** Fine. */
        FREE,
        /** Crushed: slowed and fatigued until rested. */
        CRUSHED,
        /** Too heavy to rise at all: drops everything and is pushed back down. */
        EJECTED
    }

    /** What happens when this much mass is carried up {@code levels} depths at once. */
    public static Outcome ascend(double mass, int levels) {
        double load = mass * Math.max(1, levels);
        if (load <= CAPACITY) {
            return Outcome.FREE;
        }
        if (load <= CAPACITY * 2) {
            return Outcome.CRUSHED;
        }
        return Outcome.EJECTED;
    }
}
