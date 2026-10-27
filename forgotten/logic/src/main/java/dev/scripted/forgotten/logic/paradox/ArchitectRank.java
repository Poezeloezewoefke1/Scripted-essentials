package dev.scripted.forgotten.logic.paradox;

/**
 * The Architect Codex ranks. You do not level up with experience, you level up by noticing and
 * changing reality: insight comes from witnessing anomalies, resolving them, decoding the
 * world's language, entering new dimensions and defeating the arguments the universe sends.
 * Ranks gate what the Paradox Engine will agree to do for you.
 */
public enum ArchitectRank {
    OBSERVER(0, "Observer", "Detect anomalies and read the world's integrity."),
    PATCHMAKER(25, "Patchmaker", "Repair small glitches. Patch Notes and the Stitcher answer you."),
    REWRITER(80, "Rewriter", "Change block identities, mob behaviour and local physics."),
    ARCHITECT(200, "Architect", "Build chambers, pocket depths and rules of your own."),
    PARADOX_LORD(500, "Paradox Lord", "Rewrite major rules. The Genesis Interface will listen.");

    private final int insight;
    private final String title;
    private final String power;

    ArchitectRank(int insight, String title, String power) {
        this.insight = insight;
        this.title = title;
        this.power = power;
    }

    public int insight() {
        return insight;
    }

    public String title() {
        return title;
    }

    public String power() {
        return power;
    }

    public int level() {
        return ordinal() + 1;
    }

    public static ArchitectRank of(int insight) {
        ArchitectRank result = OBSERVER;
        for (ArchitectRank rank : values()) {
            if (insight >= rank.insight) {
                result = rank;
            }
        }
        return result;
    }

    public static ArchitectRank ofLevel(int level) {
        ArchitectRank[] all = values();
        return all[Math.max(0, Math.min(all.length - 1, level - 1))];
    }

    /** Insight still needed for the next rank, or 0 at the top. */
    public int toNext(int insight) {
        if (this == PARADOX_LORD) {
            return 0;
        }
        return Math.max(0, values()[ordinal() + 1].insight - insight);
    }
}
