package dev.scripted.forgotten.player;

/**
 * A change a player made to reality that the world can later take back: a lifted island, a
 * floating lake, a chamber, an anchor. A Correction that defeats its player steals one of these.
 *
 * @param kind      what kind of edit
 * @param dimension dimension id
 * @param pos       packed BlockPos of the edit's origin
 * @param data      kind-specific data (lift height, radius)
 * @param active    whether it is still in force
 */
public record RealityEdit(Kind kind, String dimension, long pos, int data, boolean active) {
    public enum Kind {
        /** Stone remembers falling upward: a structure lifted into the air. */
        LIFT,
        /** Water remembers being a cloud: a floating sphere of water. */
        SKY_WATER,
        /** A reality anchor, rule anchor or core. */
        ANCHOR,
        /** A gravity or temporal chamber. */
        CHAMBER,
        /** Death does not apply to this player. */
        DEATHLESS,
        /** Something rewritten with the Editor. */
        REWRITE
    }

    public RealityEdit deactivated() {
        return new RealityEdit(kind, dimension, pos, data, false);
    }
}
