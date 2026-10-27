package dev.scripted.forgotten.logic.paradox;

/**
 * Debt arithmetic. Kept separate from storage so the mod's attachment holds plain numbers.
 */
public final class CausalDebt {
    /** Fraction of debt the universe collects when a player dies. */
    public static final double DEATH_COLLECTION = 0.25;
    /** Ticks without a new edit before debt starts fading. */
    public static final long QUIET_PERIOD = 6000;
    /** Debt that fades per quiet period once fading. */
    public static final double FADE = 1.0;
    /** How much one point of Coherence repays. */
    public static final double COHERENCE_VALUE = 4.0;

    private CausalDebt() {
    }

    /** Debt multiplier for an edit made in a dimension under the End's Finality. */
    public static double finalityMultiplier(boolean inEnd) {
        return inEnd ? 2.5 : 1.0;
    }

    /** Debt multiplier imposed by the "Preserve" ending: reality manipulation is heavily restricted. */
    public static double endingMultiplier(boolean preserved) {
        return preserved ? 3.0 : 1.0;
    }

    public static double afterDeath(double debt) {
        return debt * (1.0 - DEATH_COLLECTION);
    }

    public static double afterRepayment(double debt, int coherence) {
        return Math.max(0.0, debt - coherence * COHERENCE_VALUE);
    }

    /** Debt after time passes, given the tick of the last edit. */
    public static double afterTime(double debt, long lastEditTick, long now, long elapsed) {
        if (now - lastEditTick < QUIET_PERIOD) {
            return debt;
        }
        return Math.max(0.0, debt - FADE * (elapsed / (double) QUIET_PERIOD));
    }

    /** Chance per minute that a Correction is sent, by tier. */
    public static double correctionChance(DebtTier tier) {
        return switch (tier) {
            case NONE, WHISPERS, REARRANGEMENT -> 0.0;
            case CORRECTIONS -> 0.06;
            case BLEED -> 0.14;
        };
    }
}
