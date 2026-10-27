package dev.scripted.forgotten.logic.chronicle;

import java.util.Objects;

/**
 * One significant event in a world's history.
 *
 * @param event     what happened
 * @param tick      world time when it happened
 * @param dimension dimension id, e.g. "minecraft:overworld"
 * @param x         block x
 * @param y         block y
 * @param z         block z
 * @param actor     who caused or suffered it (player name, mob type), may be empty
 * @param detail    free text: death cause, item id, boss name
 */
public record ChronicleEntry(ChronicleEvent event, long tick, String dimension, int x, int y, int z,
                             String actor, String detail) {
    public ChronicleEntry {
        Objects.requireNonNull(event, "event");
        dimension = dimension == null ? "" : dimension;
        actor = actor == null ? "" : actor;
        detail = detail == null ? "" : detail;
    }

    public double distanceSq(int px, int py, int pz) {
        double dx = px - x;
        double dy = py - y;
        double dz = pz - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public long chunkKey() {
        return ((long) (x >> 4) & 0xFFFFFFFFL) | (((long) (z >> 4) & 0xFFFFFFFFL) << 32);
    }
}
