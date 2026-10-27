package dev.scripted.forgotten.logic.paradox;

/**
 * Causal Debt: what a player owes reality for contradicting it. The tiers are the escalating
 * ways the world collects. The first idea described the consequences; the "Reality Bleed" of
 * the simulation idea supplies the last tier.
 */
public enum DebtTier {
    /** Nothing owed. */
    NONE(0),
    /** Furnaces smelt before you load them, doors open before you reach them, sounds arrive early. */
    WHISPERS(20),
    /** History rearranges: felled trees return holding their own logs, mined ore veins come back. */
    REARRANGEMENT(60),
    /** Reality sends Corrections. The Second You walks into your base. */
    CORRECTIONS(120),
    /** Reality Bleed: footsteps leave holes, your name scrambles, copies try to take your place. */
    BLEED(200);

    private final int floor;

    DebtTier(int floor) {
        this.floor = floor;
    }

    public int floor() {
        return floor;
    }

    public boolean atLeast(DebtTier other) {
        return ordinal() >= other.ordinal();
    }

    public static DebtTier of(double debt) {
        DebtTier result = NONE;
        for (DebtTier tier : values()) {
            if (debt >= tier.floor) {
                result = tier;
            }
        }
        return result;
    }
}
