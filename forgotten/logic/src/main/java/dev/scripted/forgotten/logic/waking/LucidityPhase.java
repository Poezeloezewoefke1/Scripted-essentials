package dev.scripted.forgotten.logic.waking;

/**
 * The Waking World: the whole world is a sleeping mind, and the players are what is waking it.
 * Lucidity is shared by every player on the server and never decreases on its own.
 *
 * <p>Lucidity also stands in for the "Historical Pressure" of the world-memory idea: both are a
 * global value that rises with everything that happens and that makes the world react to its
 * own history. One number, so the two ideas push the same world in the same direction.
 */
public enum LucidityPhase {
    DEEP_SLEEP(0, "Deep Sleep"),
    TWITCHING(10, "Twitching"),
    HALF_AWARE(30, "Half-Aware"),
    LUCID(50, "Lucid"),
    AWAKE(70, "Awake"),
    ASCENDING(90, "Ascending"),
    /** "the world opens its eyes for the second time". Only reachable through an ending. */
    OPEN_EYES(100, "????");

    private final int floor;
    private final String title;

    LucidityPhase(int floor, String title) {
        this.floor = floor;
        this.title = title;
    }

    public int floor() {
        return floor;
    }

    public String title() {
        return title;
    }

    public boolean atLeast(LucidityPhase other) {
        return ordinal() >= other.ordinal();
    }

    public static LucidityPhase of(double lucidity) {
        LucidityPhase result = DEEP_SLEEP;
        for (LucidityPhase phase : values()) {
            if (lucidity >= phase.floor) {
                result = phase;
            }
        }
        return result;
    }
}
