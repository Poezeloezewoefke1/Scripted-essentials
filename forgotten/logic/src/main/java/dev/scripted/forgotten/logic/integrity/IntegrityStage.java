package dev.scripted.forgotten.logic.integrity;

/**
 * Memory Integrity: how well a place remembers what it is supposed to be. 100 is vanilla
 * Minecraft; 0 is a region where the rules no longer apply at all.
 *
 * <p>This merges two of the source ideas that described the same thing from different angles:
 * "Memory Integrity" and its five stages of forgetting, and "Ontological Stability" with its
 * 0-100 bands where 0 is an Unmade chunk. The stage names are the first idea's; the bands and
 * the Unmade chunk that spreads into its neighbours are the second's.
 */
public enum IntegrityStage {
    /** The world remembers. Nothing unusual happens. */
    STABLE(85, "Remembered", ""),
    /** Stage I. Odd drops, staring animals, structures that vanish when you look away. */
    DEJA_VU(65, "Déjà Vu", "I"),
    /** Stage II. Water flows up, gravity flips, sand floats, mobs appear where they should not. */
    CONTRADICTION(45, "Contradiction", "II"),
    /** Stage III. Terrain detaches, seasons overlap, doors lead to places you have been. */
    FRAGMENTATION(25, "Fragmentation", "III"),
    /** Stage IV. Blocks and mobs forget what they are. Unremembered Things appear. */
    AMNESIA(8, "Amnesia", "IV"),
    /** Stage V. The ground stops being solid; the Backside is one fall away. Spreads. */
    UNWRITTEN(0, "The Unwritten", "V");

    public static final int MAX = 100;

    private final int floor;
    private final String title;
    private final String numeral;

    IntegrityStage(int floor, String title, String numeral) {
        this.floor = floor;
        this.title = title;
        this.numeral = numeral;
    }

    /** Lowest integrity value that is still in this stage. */
    public int floor() {
        return floor;
    }

    public String title() {
        return title;
    }

    public String numeral() {
        return numeral;
    }

    /** 0 for STABLE through 5 for UNWRITTEN. */
    public int severity() {
        return ordinal();
    }

    public boolean atLeast(IntegrityStage other) {
        return ordinal() >= other.ordinal();
    }

    public static IntegrityStage of(int integrity) {
        int clamped = clamp(integrity);
        for (IntegrityStage stage : values()) {
            if (clamped >= stage.floor) {
                return stage;
            }
        }
        return UNWRITTEN;
    }

    public static int clamp(int integrity) {
        return Math.max(0, Math.min(MAX, integrity));
    }
}
