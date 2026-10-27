package dev.scripted.forgotten.logic.waking;

/**
 * Turns the world's accumulated history (pressure) into lucidity.
 *
 * <p>Pressure only ever accumulates. Lucidity is a saturating function of it, so early actions
 * wake the world quickly and the last stretch takes real commitment. Lucidity 100 cannot be
 * reached from pressure alone: the curve tops out below it. Attacking the Witness subtracts a
 * flat penalty and freezes further gains for a week of game time.
 */
public final class LucidityLedger {
    /** Pressure at which lucidity reaches half of its maximum. */
    public static final double HALF_PRESSURE = 6000.0;
    /** Highest lucidity reachable from pressure. */
    public static final double CEILING = 99.4;
    public static final long LOCK_TICKS = 7L * 24000L;

    private double pressure;
    private double penalty;
    private long lockedUntil;
    private boolean eyesOpen;

    public LucidityLedger() {
    }

    public LucidityLedger(double pressure, double penalty, long lockedUntil, boolean eyesOpen) {
        this.pressure = pressure;
        this.penalty = penalty;
        this.lockedUntil = lockedUntil;
        this.eyesOpen = eyesOpen;
    }

    public double pressure() {
        return pressure;
    }

    public double penalty() {
        return penalty;
    }

    public long lockedUntil() {
        return lockedUntil;
    }

    public boolean eyesOpen() {
        return eyesOpen;
    }

    public boolean locked(long now) {
        return now < lockedUntil;
    }

    /** Adds pressure, unless the world is refusing to wake because the Witness was attacked. */
    public boolean add(double amount, long now) {
        if (amount <= 0 || locked(now)) {
            return false;
        }
        pressure += amount;
        return true;
    }

    public double lucidity() {
        if (eyesOpen) {
            return 100.0;
        }
        double raw = CEILING * pressure / (pressure + HALF_PRESSURE);
        return Math.max(0.0, raw - penalty);
    }

    public LucidityPhase phase() {
        return LucidityPhase.of(lucidity());
    }

    /** The Witness was attacked: lucidity drops by 20 and stops rising for a week. */
    public void resent(long now) {
        penalty += 20.0;
        lockedUntil = now + LOCK_TICKS;
    }

    /** A Lullaby forces the world back toward sleep. The mercy fades as pressure grows again. */
    public void lull(double amount) {
        pressure = Math.max(0.0, pressure - amount);
    }

    /** Sets lucidity directly, by choosing the pressure that produces it. Used by commands. */
    public void setLucidity(double target) {
        double t = Math.max(0.0, Math.min(CEILING - 0.01, target));
        penalty = 0;
        eyesOpen = false;
        pressure = t * HALF_PRESSURE / (CEILING - t);
    }

    public void openEyes() {
        eyesOpen = true;
    }
}
