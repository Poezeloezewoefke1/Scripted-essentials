package dev.scripted.forgotten.logic.paradox;

/** How much insight each kind of discovery is worth. */
public enum Insight {
    ANOMALY_WITNESSED(1),
    ANOMALY_RESOLVED(3),
    WORD_DECODED(5),
    WORD_SPOKEN(4),
    OBSERVER_STARED(6),
    REALITY_EDIT(2),
    PROGRAM_COMPILED(3),
    DIMENSION_DISCOVERED(20),
    CREATURE_NAMED(10),
    BOSS_DEFEATED(60),
    MEMORY_FRAGMENT(15);

    private final int value;

    Insight(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
