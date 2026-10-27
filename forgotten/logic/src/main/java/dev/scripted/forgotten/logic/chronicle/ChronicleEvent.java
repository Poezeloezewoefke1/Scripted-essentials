package dev.scripted.forgotten.logic.chronicle;

/**
 * Kinds of things the World Chronicle records. Each kind carries how much it wakes the world
 * (pressure, feeding lucidity) and whether it is significant enough to keep as an individual
 * entry rather than only being counted.
 */
public enum ChronicleEvent {
    BLOCK_BROKEN(0.02, false),
    BLOCK_PLACED(0.02, false),
    /** Building something and never breaking it wakes the world most. Granted per placed block that survives a day. */
    BUILD_ENDURED(0.12, false),
    MOB_KILLED(0.15, false),
    ITEM_CRAFTED(0.04, false),
    SLEPT(4.0, true),
    PLAYER_DIED(6.0, true),
    NAMED_MOB_DIED(5.0, true),
    VILLAGER_DIED(3.0, true),
    EXPLOSION(1.0, true),
    ITEM_DESTROYED(0.5, true),
    STRUCTURE_DEMOLISHED(8.0, true),
    DIMENSION_ENTERED(10.0, true),
    ANOMALY_WITNESSED(1.5, true),
    ANOMALY_RESOLVED(2.0, true),
    REALITY_EDIT(5.0, true),
    BOSS_DEFEATED(250.0, true),
    DRAGON_DEFEATED(600.0, true),
    WITHER_DEFEATED(300.0, true),
    FORMATION_ANSWERED(20.0, true),
    STORM(15.0, true),
    ENDING(0.0, true);

    private final double pressure;
    private final boolean significant;

    ChronicleEvent(double pressure, boolean significant) {
        this.pressure = pressure;
        this.significant = significant;
    }

    public double pressure() {
        return pressure;
    }

    public boolean significant() {
        return significant;
    }
}
